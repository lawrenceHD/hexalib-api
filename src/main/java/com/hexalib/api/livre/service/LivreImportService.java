package com.hexalib.api.livre.service;

import com.hexalib.api.livre.dto.ImportJobDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreImportService {

    private static final int DATA_START_ROW = 4;
    private static final int BATCH_SIZE     = 50;

    private final ImportJobStore         jobStore;
    private final LivreImportLineService lineService; // bean séparé → REQUIRES_NEW fonctionne

    // ══════════════════════════════════════════════════════════════════
    // ÉTAPE 1 : Upload + parsing → création du job
    // ══════════════════════════════════════════════════════════════════

    public ImportJobDTO.ImportJobStarted parseEtCreerJob(MultipartFile file) throws IOException {
        log.info("Parsing du fichier Excel : {}", file.getOriginalFilename());

        List<ImportJobStore.LigneParsee> lignes = new ArrayList<>();

        try (Workbook wb = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = wb.getSheetAt(0);

            for (int i = DATA_START_ROW; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String categorie  = getCellString(row, 0);
                String titre      = getCellString(row, 1);
                String auteurIsbn = getCellString(row, 2);
                String editeur    = getCellString(row, 3);
                String remarque   = getCellString(row, 6);

                if (titre.isBlank()) continue;

                BigDecimal prixVente = null;
                boolean inactif = false;
                Cell prixCell = row.getCell(4);

                if (prixCell != null) {
                    if (prixCell.getCellType() == CellType.NUMERIC) {
                        double valBrute = prixCell.getNumericCellValue();
                        if (valBrute == 0.0) {
                            inactif = true;
                        } else {
                            prixVente = BigDecimal.valueOf(valBrute)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        }
                    }
                    // STRING "xx" → prixVente reste null, inactif reste false
                }

                lignes.add(new ImportJobStore.LigneParsee(
                        i + 1,
                        categorie.trim(),
                        titre.trim(),
                        auteurIsbn.isBlank() ? null : auteurIsbn.trim(),
                        editeur.isBlank()    ? null : editeur.trim(),
                        prixVente,
                        inactif,
                        remarque.isBlank()   ? null : remarque.trim()
                ));
            }
        }

        log.info("{} lignes valides parsées", lignes.size());

        String jobId        = jobStore.creerJob(lignes, BATCH_SIZE);
        int    totalBatches = (int) Math.ceil((double) lignes.size() / BATCH_SIZE);

        return ImportJobDTO.ImportJobStarted.builder()
                .jobId(jobId)
                .totalLignes(lignes.size())
                .totalBatches(totalBatches)
                .batchSize(BATCH_SIZE)
                .build();
    }

    // ══════════════════════════════════════════════════════════════════
    // ÉTAPE 2 : Traitement d'un batch
    // PAS de @Transactional ici — chaque ligne gère sa propre transaction
    // via lineService.traiterLigne() qui est REQUIRES_NEW
    // ══════════════════════════════════════════════════════════════════

    public ImportJobDTO.ImportBatchResult traiterBatch(String jobId, int batchNumero) {
        ImportJobStore.JobData job = jobStore.getJob(jobId);
        if (job == null) throw new RuntimeException("Job introuvable : " + jobId);

        List<ImportJobStore.LigneParsee> toutesLignes = job.lignes();
        int total        = toutesLignes.size();
        int totalBatches = (int) Math.ceil((double) total / BATCH_SIZE);
        int debut        = (batchNumero - 1) * BATCH_SIZE;
        int fin          = Math.min(debut + BATCH_SIZE, total);

        if (debut >= total) {
            throw new RuntimeException("Batch " + batchNumero + " hors limites");
        }

        List<ImportJobStore.LigneParsee> batch = toutesLignes.subList(debut, fin);

        // Cache partagé entre tous les batches : nom catégorie → id BDD
        Map<String, String> cacheCategories = job.cacheCategories();

        int livresAjoutes = 0;
        int livresIgnores = 0;
        List<ImportJobDTO.LigneErreur> erreurs = new ArrayList<>();

        for (ImportJobStore.LigneParsee ligne : batch) {
            try {
                // Chaque ligne dans sa propre transaction REQUIRES_NEW
                boolean ajoute = lineService.traiterLigne(ligne, cacheCategories);
                if (ajoute) livresAjoutes++;
                else        livresIgnores++;
            } catch (Exception e) {
                log.warn("Ligne {} ignorée : {}", ligne.numeroLigne(), e.getMessage());
                erreurs.add(ImportJobDTO.LigneErreur.builder()
                        .numeroLigne(ligne.numeroLigne())
                        .titre(ligne.titre())
                        .raison(e.getMessage())
                        .build());
                livresIgnores++;
            }
        }

        int traites     = fin;
        int pourcentage = (int) Math.round((double) traites / total * 100);
        boolean termine = (batchNumero >= totalBatches);

        ImportJobDTO.ImportBatchResult.ImportBatchResultBuilder builder =
                ImportJobDTO.ImportBatchResult.builder()
                        .jobId(jobId)
                        .batchNumero(batchNumero)
                        .totalBatches(totalBatches)
                        .traites(traites)
                        .total(total)
                        .pourcentage(pourcentage)
                        .termine(termine);

        if (termine) {
            builder.rapport(construireRapportFinal(total, erreurs));
            jobStore.supprimerJob(jobId);
        }

        return builder.build();
    }

    // ── Helpers ───────────────────────────────────────────────────────

    private ImportJobDTO.ImportRapportFinal construireRapportFinal(
            int totalLignes, List<ImportJobDTO.LigneErreur> erreurs) {
        return ImportJobDTO.ImportRapportFinal.builder()
                .totalLignesLues(totalLignes)
                .livresAjoutes(totalLignes - erreurs.size())
                .livresIgnores(erreurs.size())
                .erreurs(erreurs)
                .build();
    }

    private String getCellString(Row row, int col) {
        Cell cell = row.getCell(col);
        if (cell == null) return "";
        return switch (cell.getCellType()) {
            case STRING  -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default      -> "";
        };
    }
}