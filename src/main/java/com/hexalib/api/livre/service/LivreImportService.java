package com.hexalib.api.livre.service;

import com.hexalib.api.categorie.model.Categorie;
import com.hexalib.api.categorie.repository.CategorieRepository;
import com.hexalib.api.common.util.CodeGenerator;
import com.hexalib.api.livre.dto.ImportJobDTO;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreImportService {

    private static final int DATA_START_ROW = 4; // ligne 5 = index 4
    private static final int BATCH_SIZE     = 50;

    private final CategorieRepository categorieRepository;
    private final LivreRepository     livreRepository;
    private final ImportJobStore      jobStore;
    // ✅ Plus de dépendance à CategorieService — on utilise CodeGenerator directement

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

                // Ignorer les lignes sans titre
                if (titre.isBlank()) continue;

                // Traitement du prix (colonne E = index 4)
                BigDecimal prixVente = null;
                boolean inactif = false;
                Cell prixCell = row.getCell(4);

                if (prixCell != null) {
                    if (prixCell.getCellType() == CellType.NUMERIC) {
                        double valBrute = prixCell.getNumericCellValue();
                        if (valBrute == 0.0) {
                            // Prix 0 = pas à vendre → INACTIF
                            inactif = true;
                        } else {
                            // 3000 → 30.00 XAF
                            prixVente = BigDecimal.valueOf(valBrute)
                                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
                        }
                    }
                    // CellType.STRING avec "xx" → prixVente reste null, inactif reste false
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
    // ══════════════════════════════════════════════════════════════════

    @Transactional
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

        int livresAjoutes = 0;
        int livresIgnores = 0;
        List<ImportJobDTO.LigneErreur> erreurs = new ArrayList<>();

        for (ImportJobStore.LigneParsee ligne : batch) {
            try {
                boolean ajoute = traiterLigne(ligne);
                if (ajoute) livresAjoutes++;
                else        livresIgnores++;
            } catch (Exception e) {
                log.warn("Erreur ligne {} : {}", ligne.numeroLigne(), e.getMessage());
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
    // LOGIQUE PRINCIPALE : traiter une ligne
    // ══════════════════════════════════════════════════════════════════

    private boolean traiterLigne(ImportJobStore.LigneParsee ligne) {
        // 1. Vérifier ou créer la catégorie
        Categorie categorie = obtenirOuCreerCategorie(ligne.categorie());

        // 2. Vérifier doublon : titre + auteur + catégorie (via @Query)
        String auteur = ligne.auteur() != null ? ligne.auteur() : "";
        boolean existe = livreRepository
                .existsByTitreAndAuteurAndCategorieId(ligne.titre(), auteur, categorie.getId());
        if (existe) return false; // doublon ignoré silencieusement

        // 3. Créer le livre
        Livre livre = new Livre();
        livre.setTitre(ligne.titre());
        livre.setAuteur(ligne.auteur());
        livre.setMaisonEdition(ligne.maisonEdition());
        livre.setPrixVente(ligne.prixVente());
        livre.setDescription(ligne.description());
        livre.setCategorie(categorie);
        livre.setQuantiteStock(0);
        livre.setSeuilMinimal(5);
        livre.setStatut(ligne.inactif() ? Livre.Statut.INACTIF : Livre.Statut.ACTIF);
        livre.setCode(genererCodeLivre(categorie.getCode()));

        livreRepository.save(livre);
        return true;
    }

    // ══════════════════════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════════════════════

    // Cache local pour ne pas faire N fois la même requête BDD dans un batch
    private final Map<String, Categorie> cacheCategories = new HashMap<>();

    private Categorie obtenirOuCreerCategorie(String nomCategorie) {
        if (nomCategorie == null || nomCategorie.isBlank()) {
            nomCategorie = "Non classé";
        }
        final String nom = nomCategorie;

        return cacheCategories.computeIfAbsent(nom.toLowerCase(), k ->
            categorieRepository.findByNomIgnoreCase(nom)
                .orElseGet(() -> {
                    log.info("Création catégorie : {}", nom);
                    Categorie cat = new Categorie();
                    cat.setNom(nom);
                    cat.setStatut(Categorie.Statut.ACTIF);
                    cat.setCode(genererCodeCategorieUnique(nom));
                    return categorieRepository.save(cat);
                })
        );
    }

    /**
     * Même logique que CategorieService.generateUniqueCode()
     * mais sans injecter CategorieService (évite la dépendance circulaire potentielle).
     */
    private String genererCodeCategorieUnique(String nom) {
        String code;
        int attempts = 0;
        do {
            code = CodeGenerator.generateCategorieCode(nom);
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException("Impossible de générer un code unique pour : " + nom);
            }
        } while (categorieRepository.existsByCode(code));
        return code;
    }

    private String genererCodeLivre(String codeCategorie) {
        int suivant = livreRepository.countByCategorieCode(codeCategorie) + 1;
        return String.format("%s-%03d", codeCategorie, suivant);
    }

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