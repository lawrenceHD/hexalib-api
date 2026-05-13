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
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreImportService {

    private static final int DATA_START_ROW = 4; // ligne 5 dans Excel (0-indexé)
    private static final int BATCH_SIZE     = 50;

    private final ImportJobStore         jobStore;
    private final LivreImportLineService lineService;

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

                String categorie  = getCellString(row, 0).trim();
                String titre      = getCellString(row, 1).trim();
                String auteurIsbn = getCellString(row, 2).trim();
                String editeur    = getCellString(row, 3).trim();
                String remarque   = getCellString(row, 6).trim();

                // Ignorer les lignes sans titre
                if (titre.isBlank()) continue;

                // ── Colonne E : prix de vente ─────────────────────────────
                BigDecimal prixVente = parsePrixVente(row.getCell(4), titre, remarque);
                // remarque peut avoir été enrichie dans parsePrixVente via StringBuilder
                // → on la récupère via le holder
                String[] descriptionHolder = { remarque.isBlank() ? null : remarque };
                prixVente = parsePrixVenteAvecDesc(row.getCell(4), descriptionHolder);
                String descriptionFinale = descriptionHolder[0];

                // ── Colonne F : prix net → prix d'achat ───────────────────
                BigDecimal prixAchat = parsePrixAchat(row.getCell(5));

                lignes.add(new ImportJobStore.LigneParsee(
                        i + 1,
                        categorie.isBlank() ? "Non classé" : categorie,
                        titre,
                        auteurIsbn.isBlank() ? null : auteurIsbn,
                        editeur.isBlank()    ? null : editeur,
                        prixVente,
                        prixAchat,
                        false,           // inactif : toujours false, toutes les catégories sont ACTIF
                        descriptionFinale
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
        Map<String, String> cacheCategories    = job.cacheCategories();

        int livresAjoutes = 0;
        int livresIgnores = 0;
        List<ImportJobDTO.LigneErreur> erreurs = new ArrayList<>();

        for (ImportJobStore.LigneParsee ligne : batch) {
            try {
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

    // ══════════════════════════════════════════════════════════════════
    // PARSING DES PRIX
    // ══════════════════════════════════════════════════════════════════

    /**
     * Parse le prix de vente (colonne E).
     *
     * Règles :
     * - Numérique → valeur brute en XAF (PAS de division par 100)
     * - "xx" ou vide → 0 XAF
     * - "1.000/100" ou "2.000/100" → prendre la partie avant "/" (ex: 1000)
     *   et enrichir la description avec "1000 FCFA pour 100 unités"
     *
     * @param cell             cellule Excel
     * @param descriptionHolder tableau[0] contenant la description existante ;
     *                          modifié en place si format X/100 détecté
     */
    private BigDecimal parsePrixVenteAvecDesc(Cell cell, String[] descriptionHolder) {
        if (cell == null) return BigDecimal.ZERO;

        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            // Valeur 0 → prix inconnu, on met 0 (le livre reste ACTIF)
            return BigDecimal.valueOf(val).setScale(2);
        }

        if (cell.getCellType() == CellType.STRING) {
            String raw = cell.getStringCellValue().trim();

            // Cas "xx" ou vide → 0
            if (raw.isEmpty() || raw.equalsIgnoreCase("xx")) {
                return BigDecimal.ZERO;
            }

            // Cas "1.000/100" ou "2.000/100" → prix par paquet de 100
            if (raw.contains("/")) {
                String avant = raw.split("/")[0].trim();
                // Supprimer les séparateurs de milliers (point ou espace)
                avant = avant.replace(".", "").replace(" ", "").replace(",", "");
                try {
                    BigDecimal prixPaquet = new BigDecimal(avant).setScale(2);
                    // Enrichir la description
                    String noteParquet = prixPaquet.toPlainString().replace(".00", "")
                            + " FCFA pour 100 unités";
                    if (descriptionHolder[0] != null && !descriptionHolder[0].isBlank()) {
                        descriptionHolder[0] = descriptionHolder[0] + " | " + noteParquet;
                    } else {
                        descriptionHolder[0] = noteParquet;
                    }
                    return prixPaquet;
                } catch (NumberFormatException e) {
                    log.warn("Prix invalide ignoré : '{}'", raw);
                    return BigDecimal.ZERO;
                }
            }

            // Autre chaîne numérique simple (ex: "3000")
            String cleaned = raw.replace(".", "").replace(",", "").replace(" ", "");
            try {
                return new BigDecimal(cleaned).setScale(2);
            } catch (NumberFormatException e) {
                log.warn("Prix non parseable : '{}'", raw);
                return BigDecimal.ZERO;
            }
        }

        return BigDecimal.ZERO;
    }

    // Méthode legacy conservée pour compatibilité — délègue vers la version avec desc
    private BigDecimal parsePrixVente(Cell cell, String titre, String remarque) {
        String[] holder = { remarque };
        return parsePrixVenteAvecDesc(cell, holder);
    }

    /**
     * Parse le prix d'achat (colonne F = "prix net").
     * Même logique que prixVente mais sans la gestion du format X/100.
     */
    private BigDecimal parsePrixAchat(Cell cell) {
        if (cell == null) return null;

        if (cell.getCellType() == CellType.NUMERIC) {
            double val = cell.getNumericCellValue();
            if (val == 0.0) return null;
            return BigDecimal.valueOf(val).setScale(2);
        }

        if (cell.getCellType() == CellType.STRING) {
            String raw = cell.getStringCellValue().trim();
            if (raw.isEmpty() || raw.equalsIgnoreCase("xx")) return null;
            String cleaned = raw.replace(".", "").replace(",", "").replace(" ", "");
            try {
                BigDecimal val = new BigDecimal(cleaned).setScale(2);
                return val.compareTo(BigDecimal.ZERO) == 0 ? null : val;
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════

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