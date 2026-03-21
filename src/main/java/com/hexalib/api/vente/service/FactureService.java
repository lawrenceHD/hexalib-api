package com.hexalib.api.vente.service;

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
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.vente.model.LigneVente;
import com.hexalib.api.vente.model.Vente;
import com.hexalib.api.vente.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactureService {

    private final VenteRepository venteRepository;

    // Infos librairie
    private static final String LIBRAIRIE_NOM     = "HEXALIB";
    private static final String LIBRAIRIE_ADRESSE = "123 Rue du Livre";
    private static final String LIBRAIRIE_VILLE   = "Yaoundé, Cameroun";
    private static final String LIBRAIRIE_TEL     = "Tél: +237 123 456 789";

    // Couleurs
    private static final DeviceRgb PRIMARY_COLOR  = new DeviceRgb(37, 99, 235);
    private static final DeviceRgb LIGHT_GRAY     = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb BORDER_COLOR   = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(75, 85, 99);

    // Formatter montants
    private static final NumberFormat CURRENCY_FMT = NumberFormat.getInstance(Locale.FRANCE);
    static {
        CURRENCY_FMT.setMinimumFractionDigits(0);
        CURRENCY_FMT.setMaximumFractionDigits(0);
    }

    private static final DateTimeFormatter DATE_FMT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public byte[] genererFacturePDF(UUID venteId) {
        log.info("Génération de la facture PDF pour la vente: {}", venteId);

        Vente vente = venteRepository.findById(venteId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvée"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter   writer  = new PdfWriter(baos);
            PdfDocument pdfDoc  = new PdfDocument(writer);
            Document    document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            PdfFont boldFont    = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regularFont = PdfFontFactory.createFont("Helvetica");

            ajouterEntete(document, vente, boldFont, regularFont);
            ajouterTableauArticles(document, vente, boldFont, regularFont);
            ajouterTotaux(document, vente, boldFont, regularFont);
            ajouterPiedPage(document, regularFont);

            document.close();
            log.info("Facture PDF générée avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF", e);
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }
    }

    // ─── EN-TÊTE ────────────────────────────────────────────────────────────────

    private void ajouterEntete(Document doc, Vente vente,
                                PdfFont boldFont, PdfFont regularFont) {
        // Logo / Nom
        doc.add(new Paragraph(LIBRAIRIE_NOM)
                .setFont(boldFont)
                .setFontSize(24)
                .setFontColor(PRIMARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(4));

        // Adresse
        doc.add(new Paragraph(LIBRAIRIE_ADRESSE + "\n" + LIBRAIRIE_VILLE + "\n" + LIBRAIRIE_TEL)
                .setFont(regularFont)
                .setFontSize(10)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10));

        // Ligne de séparation
        doc.add(new Paragraph()
                .setBorderBottom(new SolidBorder(PRIMARY_COLOR, 2))
                .setMarginBottom(15));

        // Infos facture dans un tableau 2 colonnes
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        infoTable.addCell(infoCell("N° Facture", vente.getNumeroFacture(), boldFont, regularFont));
        infoTable.addCell(infoCell("Date", vente.getDateVente().format(DATE_FMT), boldFont, regularFont));
        infoTable.addCell(infoCell("Vendeur", vente.getVendeur().getNomComplet(), boldFont, regularFont));
        infoTable.addCell(infoCell("Statut", vente.getStatut().name(), boldFont, regularFont));

        doc.add(infoTable);
    }

    // ─── TABLEAU ARTICLES ────────────────────────────────────────────────────────

    private void ajouterTableauArticles(Document doc, Vente vente,
                                         PdfFont boldFont, PdfFont regularFont) {
        doc.add(new Paragraph("Articles")
                .setFont(boldFont)
                .setFontSize(13)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(8));

        // Colonnes : CODE | TITRE | QTÉ | P.U | RÉDUC. | TOTAL
        Table table = new Table(UnitValue.createPercentArray(new float[]{2.5f, 5f, 1.5f, 2f, 2f, 2f}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // En-têtes
        for (String h : new String[]{"CODE", "TITRE", "QTÉ", "P.U (XAF)", "RÉDUC. (XAF)", "TOTAL (XAF)"}) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(h).setFont(boldFont).setFontSize(9).setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(6)
                    .setBorder(Border.NO_BORDER));
        }

        // Lignes
        boolean alternate = false;
        for (LigneVente ligne : vente.getLignes()) {
            DeviceRgb bg = alternate ? LIGHT_GRAY : new DeviceRgb(255, 255, 255);

            table.addCell(dataCell(ligne.getCodeLivre(),                          regularFont, TextAlignment.CENTER, bg));
            table.addCell(dataCell(ligne.getTitreLivre(),                         regularFont, TextAlignment.LEFT,   bg));
            table.addCell(dataCell(String.valueOf(ligne.getQuantite()),            regularFont, TextAlignment.CENTER, bg));
            table.addCell(dataCell(formatMontant(ligne.getPrixUnitaire()),         regularFont, TextAlignment.RIGHT,  bg));
            table.addCell(dataCell(formatMontant(ligne.getMontantReduction()),     regularFont, TextAlignment.RIGHT,  bg));
            table.addCell(dataCell(formatMontant(ligne.getSousTotal()),            regularFont, TextAlignment.RIGHT,  bg));

            alternate = !alternate;
        }

        doc.add(table);
    }

    // ─── TOTAUX ──────────────────────────────────────────────────────────────────

    private void ajouterTotaux(Document doc, Vente vente,
                                PdfFont boldFont, PdfFont regularFont) {
        // Tableau des totaux aligné à droite (colonne label + colonne valeur)
        Table totauxTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth()
                .setMarginBottom(20);

        // Sous-total HT
        totauxTable.addCell(labelCell("Sous-total HT :", regularFont));
        totauxTable.addCell(valueCell(formatMontant(vente.getMontantHT()) + " XAF", regularFont));

        // Réductions
        totauxTable.addCell(labelCell("Total des réductions :", regularFont)
                .setFontColor(new DeviceRgb(16, 185, 129)));
        totauxTable.addCell(valueCell("- " + formatMontant(vente.getMontantReductions()) + " XAF", regularFont)
                .setFontColor(new DeviceRgb(16, 185, 129)));

        // Séparateur
        totauxTable.addCell(new Cell(1, 2)
                .setBorder(Border.NO_BORDER)
                .setBorderBottom(new SolidBorder(BORDER_COLOR, 1))
                .add(new Paragraph("")));

        // Total TTC — mis en valeur
        totauxTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(8)
                .add(new Paragraph("TOTAL À PAYER")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(PRIMARY_COLOR)));

        totauxTable.addCell(new Cell()
                .setBackgroundColor(PRIMARY_COLOR)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(8)
                .add(new Paragraph(formatMontant(vente.getMontantTTC()) + " XAF")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(ColorConstants.WHITE)));

        doc.add(totauxTable);
    }

    // ─── PIED DE PAGE ────────────────────────────────────────────────────────────

    private void ajouterPiedPage(Document doc, PdfFont regularFont) {
        doc.add(new Paragraph()
                .setBorderTop(new SolidBorder(BORDER_COLOR, 1))
                .setMarginTop(20)
                .setMarginBottom(10));

        doc.add(new Paragraph("Merci de votre visite !")
                .setFont(regularFont)
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(5));

        doc.add(new Paragraph("Facture non soumise à TVA - Article 293 B du CGI")
                .setFont(regularFont)
                .setFontSize(8)
                .setItalic()
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
    }

    // ─── CELLULES UTILITAIRES ────────────────────────────────────────────────────

    private Cell infoCell(String label, String value, PdfFont boldFont, PdfFont regularFont) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(8)
                .add(new Paragraph(label)
                        .setFont(boldFont).setFontSize(9).setFontColor(SECONDARY_COLOR).setMarginBottom(2))
                .add(new Paragraph(value)
                        .setFont(boldFont).setFontSize(11));
    }

    private Cell dataCell(String text, PdfFont font, TextAlignment align, DeviceRgb bg) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "—").setFont(font).setFontSize(9))
                .setTextAlignment(align)
                .setBackgroundColor(bg)
                .setPadding(6)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
    }

    private Cell labelCell(String text, PdfFont font) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(5)
                .add(new Paragraph(text).setFont(font).setFontSize(11));
    }

    private Cell valueCell(String text, PdfFont font) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .setPadding(5)
                .add(new Paragraph(text).setFont(font).setFontSize(11).setBold());
    }

    // ─── FORMATAGE ───────────────────────────────────────────────────────────────

    private String formatMontant(BigDecimal montant) {
        if (montant == null) return "0";
        return CURRENCY_FMT.format(montant);
    }
}