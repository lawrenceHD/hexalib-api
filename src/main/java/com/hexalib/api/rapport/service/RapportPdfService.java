package com.hexalib.api.rapport.service;

import com.hexalib.api.rapport.dto.*;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Slf4j
public class RapportPdfService {

    // Couleurs modernes
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(37, 99, 235); // Bleu
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(75, 85, 99); // Gris foncé
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(16, 185, 129); // Vert
    private static final DeviceRgb WARNING_COLOR = new DeviceRgb(245, 158, 11); // Orange
    private static final DeviceRgb DANGER_COLOR = new DeviceRgb(239, 68, 68); // Rouge

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(Locale.FRANCE);

    static {
        CURRENCY_FORMATTER.setMinimumFractionDigits(0);
        CURRENCY_FORMATTER.setMaximumFractionDigits(0);
    }

    /**
     * Générer le PDF du rapport journalier
     */
    public byte[] genererRapportJournalierPDF(RapportJournalierDTO rapport) {
        log.info("Génération du PDF du rapport journalier pour le {}", rapport.getDate());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // Polices
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regularFont = PdfFontFactory.createFont("Helvetica");

            // En-tête
            addHeaderRapport(document, "RAPPORT DE CLÔTURE JOURNALIÈRE", 
                    rapport.getDate().format(DATE_FORMATTER), boldFont, regularFont);

            // KPIs principaux
            addKPIsSection(document, rapport, boldFont, regularFont);

            // CA par vendeur
            if (rapport.getCaParVendeur() != null && !rapport.getCaParVendeur().isEmpty()) {
                addPerformanceVendeursSection(document, rapport.getCaParVendeur(), boldFont, regularFont);
            }

            // Top 5 livres
            if (rapport.getTopLivres() != null && !rapport.getTopLivres().isEmpty()) {
                addTopLivresSection(document, rapport.getTopLivres(), boldFont, regularFont);
            }

            // Top 3 catégories
            if (rapport.getTopCategories() != null && !rapport.getTopCategories().isEmpty()) {
                addTopCategoriesSection(document, rapport.getTopCategories(), boldFont, regularFont);
            }

            // Alertes stock critique
            if (rapport.getAlertesStock() != null && !rapport.getAlertesStock().isEmpty()) {
                addAlertesStockSection(document, rapport.getAlertesStock(), boldFont, regularFont);
            }

            // Pied de page
            addFooter(document, regularFont);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF du rapport journalier", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    /**
     * Générer le PDF du rapport périodique
     */
    public byte[] genererRapportPeriodiquePDF(RapportPeriodiqueDTO rapport) {
        log.info("Génération du PDF du rapport {} du {} au {}", 
                rapport.getPeriode(), rapport.getDateDebut(), rapport.getDateFin());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regularFont = PdfFontFactory.createFont("Helvetica");

            // En-tête
            String sousTitre = String.format("Du %s au %s", 
                    rapport.getDateDebut().format(DATE_FORMATTER),
                    rapport.getDateFin().format(DATE_FORMATTER));
            addHeaderRapport(document, "RAPPORT " + rapport.getPeriode(), sousTitre, boldFont, regularFont);

            // KPIs principaux
            addKPIsPeriodique(document, rapport, boldFont, regularFont);

            // Comparaison période précédente
            if (rapport.getEvolutionCA() != null) {
                addComparaisonPeriode(document, rapport, boldFont, regularFont);
            }

            // Top 10 livres
            if (rapport.getTopLivres() != null && !rapport.getTopLivres().isEmpty()) {
                addTopLivresSection(document, rapport.getTopLivres(), boldFont, regularFont);
            }

            // Top 5 catégories
            if (rapport.getTopCategories() != null && !rapport.getTopCategories().isEmpty()) {
                addTopCategoriesSection(document, rapport.getTopCategories(), boldFont, regularFont);
            }

            // Performance vendeurs
            if (rapport.getPerformanceVendeurs() != null && !rapport.getPerformanceVendeurs().isEmpty()) {
                addPerformanceVendeursSection(document, rapport.getPerformanceVendeurs(), boldFont, regularFont);
            }

            // Analyse réductions
            if (rapport.getAnalyseReductions() != null) {
                addAnalyseReductionsSection(document, rapport.getAnalyseReductions(), boldFont, regularFont);
            }

            // Rotation stock
            if (rapport.getRotationStock() != null && !rapport.getRotationStock().isEmpty()) {
                addRotationStockSection(document, rapport.getRotationStock(), boldFont, regularFont);
            }

            addFooter(document, regularFont);
            document.close();

            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF du rapport périodique", e);
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    // ==================== SECTIONS DU PDF ====================

    private void addHeaderRapport(Document document, String titre, String sousTitre, 
                                   PdfFont boldFont, PdfFont regularFont) {
        // Logo / Nom entreprise
        Paragraph company = new Paragraph("HEXALIB")
                .setFont(boldFont)
                .setFontSize(24)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(company);

        // Titre du rapport
        Paragraph title = new Paragraph(titre)
                .setFont(boldFont)
                .setFontSize(18)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5);
        document.add(title);

        // Sous-titre (date)
        Paragraph subtitle = new Paragraph(sousTitre)
                .setFont(regularFont)
                .setFontSize(12)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10);
        document.add(subtitle);

        // Ligne de séparation
        document.add(new Paragraph()
                .setBorderBottom(new SolidBorder(PRIMARY_COLOR, 2))
                .setMarginBottom(20));
    }

    private void addKPIsSection(Document document, RapportJournalierDTO rapport, 
                                 PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("INDICATEURS CLÉS")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10));

        Table kpiTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Ligne 1
        kpiTable.addCell(createKPICell("Nombre de ventes", 
                String.valueOf(rapport.getNombreVentes()), boldFont, regularFont));
        kpiTable.addCell(createKPICell("Chiffre d'affaires", 
                formatCurrency(rapport.getChiffreAffaires()) + " FCFA", boldFont, regularFont));
        kpiTable.addCell(createKPICell("Réductions accordées", 
                formatCurrency(rapport.getMontantReductions()) + " FCFA", boldFont, regularFont));
        kpiTable.addCell(createKPICell("Livres vendus", 
                String.valueOf(rapport.getNombreLivresVendus()), boldFont, regularFont));

        document.add(kpiTable);
    }

    private void addKPIsPeriodique(Document document, RapportPeriodiqueDTO rapport, 
                                    PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("INDICATEURS CLÉS")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10));

        Table kpiTable = new Table(UnitValue.createPercentArray(new float[]{1, 1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(10);

        // Ligne 1
        kpiTable.addCell(createKPICell("Nombre de ventes", 
                String.valueOf(rapport.getNombreVentes()), boldFont, regularFont));
        kpiTable.addCell(createKPICell("Chiffre d'affaires", 
                formatCurrency(rapport.getChiffreAffaires()) + " FCFA", boldFont, regularFont));
        kpiTable.addCell(createKPICell("Livres vendus", 
                String.valueOf(rapport.getNombreLivresVendus()), boldFont, regularFont));

        // Ligne 2
        kpiTable.addCell(createKPICell("Réductions", 
                formatCurrency(rapport.getMontantReductions()) + " FCFA", boldFont, regularFont));
        kpiTable.addCell(createKPICell("Marge bénéficiaire", 
                formatCurrency(rapport.getMargeBeneficiaire()) + " FCFA", boldFont, regularFont));
        
        BigDecimal panierMoyen = rapport.getNombreVentes() > 0 
                ? rapport.getChiffreAffaires().divide(BigDecimal.valueOf(rapport.getNombreVentes()), 0, java.math.RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        kpiTable.addCell(createKPICell("Panier moyen", 
                formatCurrency(panierMoyen) + " FCFA", boldFont, regularFont));

        document.add(kpiTable);
    }

    private void addComparaisonPeriode(Document document, RapportPeriodiqueDTO rapport, 
                                        PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("COMPARAISON PÉRIODE PRÉCÉDENTE")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table compTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Évolution CA
        DeviceRgb caColor = rapport.getEvolutionCA().compareTo(BigDecimal.ZERO) >= 0 
                ? SUCCESS_COLOR : DANGER_COLOR;
        String caSign = rapport.getEvolutionCA().compareTo(BigDecimal.ZERO) >= 0 ? "+" : "";
        
        compTable.addCell(createComparisonCell("Évolution CA", 
                caSign + rapport.getEvolutionCA().setScale(2, java.math.RoundingMode.HALF_UP) + "%",
                caColor, boldFont, regularFont));

        // Évolution nombre ventes
        DeviceRgb ventesColor = rapport.getEvolutionNombreVentes() >= 0 
                ? SUCCESS_COLOR : DANGER_COLOR;
        String ventesSign = rapport.getEvolutionNombreVentes() >= 0 ? "+" : "";
        
        compTable.addCell(createComparisonCell("Évolution ventes", 
                ventesSign + rapport.getEvolutionNombreVentes(),
                ventesColor, boldFont, regularFont));

        document.add(compTable);
    }

    private void addPerformanceVendeursSection(Document document, List<PerformanceVendeurDTO> vendeurs,
                                                PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("PERFORMANCE DES VENDEURS")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 2, 2, 2}))
                .useAllAvailableWidth();

        // En-têtes
        String[] headers = {"Rang", "Vendeur", "Ventes", "CA (FCFA)", "Panier Moyen"};
        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header, boldFont));
        }

        // Données
        boolean alternate = false;
        for (PerformanceVendeurDTO vendeur : vendeurs) {
            DeviceRgb bgColor = alternate ? LIGHT_GRAY : WHITE;
            
            table.addCell(createTableCell(String.valueOf(vendeur.getRang()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(vendeur.getNomComplet(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(String.valueOf(vendeur.getNombreVentes()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(formatCurrency(vendeur.getChiffreAffaires()), regularFont, TextAlignment.RIGHT, bgColor));
            table.addCell(createTableCell(formatCurrency(vendeur.getPanierMoyen()), regularFont, TextAlignment.RIGHT, bgColor));

            alternate = !alternate;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTopLivresSection(Document document, List<TopLivreDTO> livres,
                                      PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("TOP LIVRES VENDUS")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 2, 4, 3, 2, 2}))
                .useAllAvailableWidth();

        // En-têtes
        String[] headers = {"Rang", "Code", "Titre", "Auteur", "Qté", "CA (FCFA)"};
        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header, boldFont));
        }

        // Données
        boolean alternate = false;
        for (TopLivreDTO livre : livres) {
            DeviceRgb bgColor = alternate ? LIGHT_GRAY : WHITE;
            
            table.addCell(createTableCell(String.valueOf(livre.getRang()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(livre.getCode(), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(livre.getTitre(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(livre.getAuteur(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(String.valueOf(livre.getQuantiteVendue()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(formatCurrency(livre.getChiffreAffaires()), regularFont, TextAlignment.RIGHT, bgColor));

            alternate = !alternate;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addTopCategoriesSection(Document document, List<TopCategorieDTO> categories,
                                          PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("TOP CATÉGORIES")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 4, 2, 2}))
                .useAllAvailableWidth();

        // En-têtes
        String[] headers = {"Rang", "Catégorie", "Qté Vendue", "CA (FCFA)"};
        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header, boldFont));
        }

        // Données
        boolean alternate = false;
        for (TopCategorieDTO categorie : categories) {
            DeviceRgb bgColor = alternate ? LIGHT_GRAY : WHITE;
            
            table.addCell(createTableCell(String.valueOf(categorie.getRang()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(categorie.getNom(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(String.valueOf(categorie.getQuantiteVendue()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(formatCurrency(categorie.getChiffreAffaires()), regularFont, TextAlignment.RIGHT, bgColor));

            alternate = !alternate;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addAlertesStockSection(Document document, List<LivreStockCritiqueDTO> alertes,
                                         PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("⚠ ALERTES STOCK CRITIQUE")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(DANGER_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{2, 4, 3, 2, 2, 2}))
                .useAllAvailableWidth();

        // En-têtes
        String[] headers = {"Code", "Titre", "Auteur", "Stock", "Seuil", "Statut"};
        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header, boldFont));
        }

        // Données
        for (LivreStockCritiqueDTO livre : alertes) {
            DeviceRgb bgColor = "RUPTURE".equals(livre.getStatutStock()) 
                    ? new DeviceRgb(254, 226, 226) // Rouge clair
                    : new DeviceRgb(254, 243, 199); // Orange clair
            
            table.addCell(createTableCell(livre.getCode(), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(livre.getTitre(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(livre.getAuteur(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(String.valueOf(livre.getQuantiteStock()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(String.valueOf(livre.getSeuilMinimal()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(livre.getStatutStock(), regularFont, TextAlignment.CENTER, bgColor));
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addAnalyseReductionsSection(Document document, AnalyseReductionsDTO analyse,
                                              PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("ANALYSE DES RÉDUCTIONS")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        table.addCell(createKPICell("Montant total réductions", 
                formatCurrency(analyse.getMontantTotalReductions()) + " FCFA", boldFont, regularFont));
        table.addCell(createKPICell("Ventes avec réduction", 
                analyse.getNombreVentesAvecReduction() + " (" + 
                analyse.getPourcentageVentesAvecReduction().setScale(1, java.math.RoundingMode.HALF_UP) + "%)", 
                boldFont, regularFont));
        table.addCell(createKPICell("Réduction moyenne", 
                formatCurrency(analyse.getReductionMoyenne()) + " FCFA", boldFont, regularFont));
        table.addCell(createKPICell("Réduction maximale", 
                formatCurrency(analyse.getReductionMaximale()) + " FCFA", boldFont, regularFont));

        document.add(table);
    }

    private void addRotationStockSection(Document document, List<RotationStockDTO> rotations,
                                          PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("ROTATION DU STOCK PAR CATÉGORIE")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginTop(10)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{4, 2, 2, 2}))
                .useAllAvailableWidth();

        // En-têtes
        String[] headers = {"Catégorie", "Qté Vendue", "Stock Actuel", "Taux Rotation (%)"};
        for (String header : headers) {
            table.addHeaderCell(createHeaderCell(header, boldFont));
        }

        // Données
        boolean alternate = false;
        for (RotationStockDTO rotation : rotations) {
            DeviceRgb bgColor = alternate ? LIGHT_GRAY : WHITE;
            
            table.addCell(createTableCell(rotation.getCategorieNom(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(String.valueOf(rotation.getQuantiteVendue()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(String.valueOf(rotation.getStockActuel()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(rotation.getTauxRotation().setScale(2, java.math.RoundingMode.HALF_UP).toString(), 
                    regularFont, TextAlignment.CENTER, bgColor));

            alternate = !alternate;
        }

        document.add(table);
        document.add(new Paragraph("\n"));
    }

    private void addFooter(Document document, PdfFont regularFont) {
        document.add(new Paragraph()
                .setBorderTop(new SolidBorder(BORDER_COLOR, 1))
                .setMarginTop(20)
                .setMarginBottom(10));

        document.add(new Paragraph("Rapport généré automatiquement par HEXALIB le " + 
                java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ==================== CELLULES UTILITAIRES ====================

    private Cell createKPICell(String label, String value, PdfFont boldFont, PdfFont regularFont) {
        return new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(10)
                .add(new Paragraph(label)
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR)
                        .setMarginBottom(3))
                .add(new Paragraph(value)
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(PRIMARY_COLOR));
    }

    private Cell createComparisonCell(String label, String value, DeviceRgb valueColor,
                                       PdfFont boldFont, PdfFont regularFont) {
        return new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(10)
                .add(new Paragraph(label)
                        .setFont(regularFont)
                        .setFontSize(9)
                        .setFontColor(SECONDARY_COLOR)
                        .setMarginBottom(3))
                .add(new Paragraph(value)
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(valueColor));
    }

    private Cell createHeaderCell(String text, PdfFont boldFont) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(boldFont)
                        .setFontSize(10))
                .setBackgroundColor(PRIMARY_COLOR)
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8)
                .setBorder(Border.NO_BORDER);
    }

    private Cell createTableCell(String text, PdfFont font, TextAlignment alignment, DeviceRgb bgColor) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(9))
                .setTextAlignment(alignment)
                .setBackgroundColor(bgColor)
                .setPadding(6)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
    }

    private String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "0";
        }
        return CURRENCY_FORMATTER.format(amount.doubleValue());
    }
}