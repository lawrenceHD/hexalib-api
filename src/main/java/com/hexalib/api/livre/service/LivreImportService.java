package com.hexalib.api.livre.service;

import com.hexalib.api.categorie.model.Categorie;
import com.hexalib.api.categorie.repository.CategorieRepository;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.common.util.CodeGenerator;
import com.hexalib.api.livre.dto.ImportResultResponse;
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
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreImportService {

    private final LivreRepository      livreRepository;
    private final CategorieRepository  categorieRepository;
    private final LivreService         livreService;

    // Indices des colonnes du template (0-based)
    private static final int COL_TITRE          = 0;
    private static final int COL_AUTEUR         = 1;
    private static final int COL_MAISON_EDITION = 2;
    private static final int COL_LANGUE         = 3;
    private static final int COL_PRIX_VENTE     = 4;
    private static final int COL_QUANTITE_STOCK = 5;
    private static final int COL_SEUIL_MINIMAL  = 6;
    private static final int COL_ISBN           = 7;
    private static final int COL_DATE_PARUTION  = 8;
    private static final int COL_EMPLACEMENT    = 9;
    private static final int COL_DESCRIPTION    = 10;

    // Ligne de départ des données (0-based) : lignes 0=titre doc, 1=instructions, 2=en-têtes → données à partir de 3
    private static final int DATA_START_ROW = 3;

    @Transactional
    public ImportResultResponse importLivres(MultipartFile file, String categorieId) {
        log.info("Début import livres pour catégorie {}", categorieId);

        // Vérifier le fichier
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("Le fichier Excel est obligatoire");
        }
        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".xlsx") && !filename.endsWith(".xls"))) {
            throw new BadRequestException("Le fichier doit être au format Excel (.xlsx ou .xls)");
        }

        // Récupérer la catégorie
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", categorieId));

        // Résultats
        int totalLignes      = 0;
        int livresAjoutes    = 0;
        int lignesIncompletes = 0;
        List<ImportResultResponse.DoublonInfo> doublons        = new ArrayList<>();
        List<String>                           lignesIgnorees  = new ArrayList<>();

        try (Workbook workbook = new XSSFWorkbook(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int rowIdx = DATA_START_ROW; rowIdx <= sheet.getLastRowNum(); rowIdx++) {
                Row row = sheet.getRow(rowIdx);
                if (row == null) continue;

                // Ignorer les lignes entièrement vides
                if (isRowEmpty(row)) continue;

                totalLignes++;
                int excelLine = rowIdx + 1; // Numéro de ligne lisible (1-based)

                // ── Lire les champs obligatoires ──
                String titre         = getCellString(row, COL_TITRE);
                String auteur        = getCellString(row, COL_AUTEUR);
                String maisonEdition = getCellString(row, COL_MAISON_EDITION);
                String langue        = getCellString(row, COL_LANGUE);
                BigDecimal prixVente = getCellDecimal(row, COL_PRIX_VENTE);
                Integer quantiteStock = getCellInt(row, COL_QUANTITE_STOCK);
                Integer seuilMinimal  = getCellInt(row, COL_SEUIL_MINIMAL);

                // ── Vérifier complétude des champs obligatoires ──
                if (isBlank(titre) || isBlank(auteur) || isBlank(maisonEdition)
                        || isBlank(langue) || prixVente == null
                        || quantiteStock == null || seuilMinimal == null) {

                    lignesIncompletes++;
                    String detail = String.format("Ligne %d ignorée (champs obligatoires manquants) : titre='%s'",
                            excelLine, titre != null ? titre : "");
                    lignesIgnorees.add(detail);
                    log.debug(detail);
                    continue;
                }

                // ── Vérifier doublon : titre + auteur + prixVente ──
                if (livreRepository.existsByTitreAndAuteurAndPrixVente(titre, auteur, prixVente)) {
                    doublons.add(ImportResultResponse.DoublonInfo.builder()
                            .titre(titre)
                            .auteur(auteur)
                            .prixVente(prixVente.toPlainString() + " XAF")
                            .numeroLigne(excelLine)
                            .build());
                    log.debug("Doublon détecté ligne {} : {} - {}", excelLine, titre, auteur);
                    continue;
                }

                // ── Lire les champs optionnels ──
                String    isbn          = getCellString(row, COL_ISBN);
                String    dateParutionStr = getCellString(row, COL_DATE_PARUTION);
                String    emplacement   = getCellString(row, COL_EMPLACEMENT);
                String    description   = getCellString(row, COL_DESCRIPTION);
                LocalDate dateParution  = parseDate(dateParutionStr);

                // Vérifier ISBN unique si fourni
                if (!isBlank(isbn) && livreRepository.existsByIsbn(isbn)) {
                    isbn = null; // Ignorer l'ISBN en doublon plutôt que bloquer
                    log.warn("ISBN doublon ignoré ligne {}", excelLine);
                }

                // ── Générer le code unique ──
                String code = generateUniqueLivreCode(categorie);

                // ── Créer le livre ──
                Livre livre = new Livre();
                livre.setCode(code);
                livre.setTitre(titre);
                livre.setAuteur(auteur);
                livre.setMaisonEdition(maisonEdition);
                livre.setLangue(langue);
                livre.setPrixVente(prixVente);
                livre.setQuantiteStock(quantiteStock);
                livre.setSeuilMinimal(seuilMinimal);
                livre.setCategorie(categorie);
                livre.setStatut(Livre.Statut.ACTIF);
                if (!isBlank(isbn))        livre.setIsbn(isbn);
                if (dateParution != null)  livre.setDateParution(dateParution);
                if (!isBlank(emplacement)) livre.setEmplacement(emplacement);
                if (!isBlank(description)) livre.setDescription(description);

                livreRepository.save(livre);
                livresAjoutes++;
            }

        } catch (IOException e) {
            log.error("Erreur lecture fichier Excel", e);
            throw new BadRequestException("Erreur lors de la lecture du fichier Excel : " + e.getMessage());
        }

        log.info("Import terminé : {} ajoutés, {} incomplets, {} doublons sur {} lignes lues",
                livresAjoutes, lignesIncompletes, doublons.size(), totalLignes);

        return ImportResultResponse.builder()
                .totalLignesLues(totalLignes)
                .livresAjoutes(livresAjoutes)
                .lignesIncompletes(lignesIncompletes)
                .doublonsTrouves(doublons.size())
                .doublons(doublons)
                .lignesIgnoreesDetail(lignesIgnorees)
                .build();
    }

    // ── Génération code unique ────────────────────────────────────────────────

    private String generateUniqueLivreCode(Categorie categorie) {
        String code;
        int counter = 1;
        do {
            code = String.format("%s-%03d", categorie.getCode(), counter++);
            if (counter > 9999) throw new BadRequestException("Impossible de générer un code unique");
        } while (livreRepository.existsByCode(code));
        return code;
    }

    // ── Helpers de lecture cellules ───────────────────────────────────────────

    private String getCellString(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        switch (cell.getCellType()) {
            case STRING:  return cell.getStringCellValue().trim();
            case NUMERIC: return String.valueOf((long) cell.getNumericCellValue()).trim();
            case BOOLEAN: return String.valueOf(cell.getBooleanCellValue());
            default:      return null;
        }
    }

    private BigDecimal getCellDecimal(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return BigDecimal.valueOf(cell.getNumericCellValue());
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim().replaceAll("[^0-9.]", "");
                return isBlank(val) ? null : new BigDecimal(val);
            }
        } catch (Exception e) {
            log.debug("Impossible de lire décimal colonne {}", colIdx);
        }
        return null;
    }

    private Integer getCellInt(Row row, int colIdx) {
        Cell cell = row.getCell(colIdx, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        if (cell == null) return null;
        try {
            if (cell.getCellType() == CellType.NUMERIC) {
                return (int) cell.getNumericCellValue();
            }
            if (cell.getCellType() == CellType.STRING) {
                String val = cell.getStringCellValue().trim();
                return isBlank(val) ? null : Integer.parseInt(val);
            }
        } catch (Exception e) {
            log.debug("Impossible de lire entier colonne {}", colIdx);
        }
        return null;
    }

    private LocalDate parseDate(String dateStr) {
        if (isBlank(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        } catch (Exception e) {
            try {
                return LocalDate.parse(dateStr.trim(), DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            } catch (Exception e2) {
                return null;
            }
        }
    }

    private boolean isRowEmpty(Row row) {
        for (int c = COL_TITRE; c <= COL_DESCRIPTION; c++) {
            Cell cell = row.getCell(c, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
            if (cell != null && cell.getCellType() != CellType.BLANK) {
                String val = getCellString(row, c);
                if (!isBlank(val)) return false;
            }
        }
        return true;
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}