package com.hexalib.api.commande.service;

import com.hexalib.api.commande.model.CommandeFournisseur;
import com.hexalib.api.commande.model.LigneCommandeFournisseur;
import com.hexalib.api.commande.repository.CommandeFournisseurRepository;
import com.hexalib.api.common.exception.ResourceNotFoundException;
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
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class CommandePdfService {

    private final CommandeFournisseurRepository commandeRepository;

    // Couleurs modernes
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(99, 102, 241); // Indigo
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(75, 85, 99); // Gray
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(249, 250, 251);
    private static final DeviceRgb BORDER_COLOR = new DeviceRgb(229, 231, 235);
    private static final DeviceRgb WHITE = new DeviceRgb(255, 255, 255);

    // Formatters
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final NumberFormat CURRENCY_FORMATTER = NumberFormat.getInstance(Locale.FRANCE);

    static {
        CURRENCY_FORMATTER.setMinimumFractionDigits(0);
        CURRENCY_FORMATTER.setMaximumFractionDigits(0);
    }

    public byte[] generateCommandePdf(String commandeId) {
        CommandeFournisseur commande = commandeRepository.findById(commandeId)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", commandeId));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);
            document.setMargins(40, 40, 40, 40);

            // Polices
            PdfFont boldFont = PdfFontFactory.createFont("Helvetica-Bold");
            PdfFont regularFont = PdfFontFactory.createFont("Helvetica");

            // En-tête
            addHeader(document, commande, boldFont, regularFont);

            // Informations de la commande
            addCommandeInfo(document, commande, boldFont, regularFont);

            // Informations du fournisseur
            addFournisseurInfo(document, commande, boldFont, regularFont);

            // Tableau des articles
            addArticlesTable(document, commande, boldFont, regularFont);

            // Totaux
            addTotals(document, commande, boldFont, regularFont);

            // Pied de page
            addFooter(document, commande, regularFont);

            document.close();
            return baos.toByteArray();

        } catch (Exception e) {
            throw new RuntimeException("Erreur lors de la génération du PDF", e);
        }
    }

    private void addHeader(Document document, CommandeFournisseur commande, PdfFont boldFont, PdfFont regularFont) {
        Table headerTable = new Table(UnitValue.createPercentArray(new float[]{2, 1}));
        headerTable.setWidth(UnitValue.createPercentValue(100));

        // Colonne gauche - Informations société
        Cell leftCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("HEXALIB")
                        .setFont(boldFont)
                        .setFontSize(24)
                        .setFontColor(PRIMARY_COLOR)
                        .setMarginBottom(5))
                .add(new Paragraph("Système de Gestion de Librairie")
                        .setFont(regularFont)
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR));

        // Colonne droite - Titre et numéro
        Cell rightCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("BON DE COMMANDE")
                        .setFont(boldFont)
                        .setFontSize(16)
                        .setFontColor(PRIMARY_COLOR)
                        .setMarginBottom(5))
                .add(new Paragraph(commande.getNumeroCommande())
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(SECONDARY_COLOR));

        headerTable.addCell(leftCell);
        headerTable.addCell(rightCell);
        document.add(headerTable);

        // Ligne de séparation
        document.add(new Paragraph()
                .setBorderBottom(new SolidBorder(PRIMARY_COLOR, 2))
                .setMarginTop(10)
                .setMarginBottom(20));
    }

    private void addCommandeInfo(Document document, CommandeFournisseur commande, PdfFont boldFont, PdfFont regularFont) {
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}));
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(15);

        // Date de commande
        infoTable.addCell(createInfoCell("Date de commande", 
            commande.getDateCommande().format(DATE_FORMATTER), boldFont, regularFont));

        // Date de réception prévue
        String dateReceptionPrevue = commande.getDateReceptionPrevue() != null 
            ? commande.getDateReceptionPrevue().format(DATE_FORMATTER) 
            : "Non spécifiée";
        infoTable.addCell(createInfoCell("Réception prévue", dateReceptionPrevue, boldFont, regularFont));

        // Statut
        String statutColor = getStatutColor(commande.getStatut());
        Cell statutCell = new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(10)
                .add(new Paragraph("Statut")
                        .setFont(boldFont)
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR)
                        .setMarginBottom(3))
                .add(new Paragraph(commande.getStatut().toString())
                        .setFont(boldFont)
                        .setFontSize(11)
                        .setFontColor(ColorConstants.BLACK));
        infoTable.addCell(statutCell);

        // Créé par
        String createdBy = commande.getCreatedBy() != null 
            ? commande.getCreatedBy().getNomComplet() 
            : "Non renseigné";
        infoTable.addCell(createInfoCell("Créé par", createdBy, boldFont, regularFont));

        document.add(infoTable);
    }

    private void addFournisseurInfo(Document document, CommandeFournisseur commande, PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("Fournisseur")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10));

        Table fournisseurTable = new Table(1);
        fournisseurTable.setWidth(UnitValue.createPercentValue(100));
        fournisseurTable.setMarginBottom(20);

        Cell fournisseurCell = new Cell()
                .setBorder(new SolidBorder(BORDER_COLOR, 1))
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(15);

        fournisseurCell.add(new Paragraph(commande.getFournisseur().getNom())
                .setFont(boldFont)
                .setFontSize(12)
                .setMarginBottom(5));

        if (commande.getFournisseur().getContact() != null) {
            fournisseurCell.add(new Paragraph("Contact: " + commande.getFournisseur().getContact())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(SECONDARY_COLOR));
        }

        if (commande.getFournisseur().getTelephone() != null) {
            fournisseurCell.add(new Paragraph("Tél: " + commande.getFournisseur().getTelephone())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(SECONDARY_COLOR));
        }

        if (commande.getFournisseur().getEmail() != null) {
            fournisseurCell.add(new Paragraph("Email: " + commande.getFournisseur().getEmail())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(SECONDARY_COLOR));
        }

        if (commande.getFournisseur().getAdresse() != null) {
            fournisseurCell.add(new Paragraph("Adresse: " + commande.getFournisseur().getAdresse())
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(SECONDARY_COLOR)
                    .setMarginTop(5));
        }

        fournisseurTable.addCell(fournisseurCell);
        document.add(fournisseurTable);
    }

    private void addArticlesTable(Document document, CommandeFournisseur commande, PdfFont boldFont, PdfFont regularFont) {
        document.add(new Paragraph("Articles commandés")
                .setFont(boldFont)
                .setFontSize(14)
                .setFontColor(PRIMARY_COLOR)
                .setMarginBottom(10));

        Table table = new Table(UnitValue.createPercentArray(new float[]{1, 3, 1.5f, 1.5f, 1.5f}));
        table.setWidth(UnitValue.createPercentValue(100));

        // En-tête du tableau
        String[] headers = {"Code", "Désignation", "Qté", "P.U (FCFA)", "Total (FCFA)"};
        for (String header : headers) {
            table.addHeaderCell(new Cell()
                    .add(new Paragraph(header)
                            .setFont(boldFont)
                            .setFontSize(10)
                            .setFontColor(ColorConstants.WHITE))
                    .setBackgroundColor(PRIMARY_COLOR)
                    .setTextAlignment(TextAlignment.CENTER)
                    .setPadding(8)
                    .setBorder(Border.NO_BORDER));
        }

        // Lignes du tableau
        boolean alternate = false;
for (LigneCommandeFournisseur ligne : commande.getLignes()) {
            DeviceRgb bgColor = alternate ? LIGHT_GRAY : WHITE;
            
            table.addCell(createTableCell(ligne.getLivre().getCode(), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(ligne.getLivre().getTitre(), regularFont, TextAlignment.LEFT, bgColor));
            table.addCell(createTableCell(String.valueOf(ligne.getQuantite()), regularFont, TextAlignment.CENTER, bgColor));
            table.addCell(createTableCell(formatCurrency(ligne.getPrixAchatUnitaire()), regularFont, TextAlignment.RIGHT, bgColor));
            table.addCell(createTableCell(formatCurrency(ligne.getSousTotal()), regularFont, TextAlignment.RIGHT, bgColor));

            alternate = !alternate;
        }

        document.add(table);
    }

    private void addTotals(Document document, CommandeFournisseur commande, PdfFont boldFont, PdfFont regularFont) {
        Table totalTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}));
        totalTable.setWidth(UnitValue.createPercentValue(100));
        totalTable.setMarginTop(20);

        // Ligne vide pour espacer
        totalTable.addCell(new Cell().setBorder(Border.NO_BORDER));
        totalTable.addCell(new Cell().setBorder(Border.NO_BORDER));

        // Total
        totalTable.addCell(new Cell()
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.RIGHT)
                .add(new Paragraph("MONTANT TOTAL:")
                        .setFont(boldFont)
                        .setFontSize(12)
                        .setFontColor(PRIMARY_COLOR))
                .setPaddingRight(10));

        totalTable.addCell(new Cell()
                .setBackgroundColor(PRIMARY_COLOR)
                .setBorder(Border.NO_BORDER)
                .setTextAlignment(TextAlignment.CENTER)
                .add(new Paragraph(formatCurrency(commande.getMontantTotal()) + " FCFA")
                        .setFont(boldFont)
                        .setFontSize(14)
                        .setFontColor(ColorConstants.WHITE))
                .setPadding(10));

        document.add(totalTable);
    }

    private void addFooter(Document document, CommandeFournisseur commande, PdfFont regularFont) {
        if (commande.getNotes() != null && !commande.getNotes().isEmpty()) {
            document.add(new Paragraph()
                    .setBorderTop(new SolidBorder(BORDER_COLOR, 1))
                    .setMarginTop(30)
                    .setMarginBottom(10));

            document.add(new Paragraph("Notes:")
                    .setFont(regularFont)
                    .setFontSize(10)
                    .setFontColor(SECONDARY_COLOR)
                    .setBold()
                    .setMarginBottom(5));

            document.add(new Paragraph(commande.getNotes())
                    .setFont(regularFont)
                    .setFontSize(9)
                    .setFontColor(SECONDARY_COLOR)
                    .setBackgroundColor(LIGHT_GRAY)
                    .setPadding(10)
                    .setMarginBottom(20));
        }

        // Ligne de signature
        document.add(new Paragraph()
                .setBorderTop(new SolidBorder(BORDER_COLOR, 1))
                .setMarginTop(30)
                .setMarginBottom(20));

        document.add(new Paragraph("Document généré automatiquement par HEXALIB")
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));

        document.add(new Paragraph(commande.getCreatedAt().format(DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")))
                .setFont(regularFont)
                .setFontSize(8)
                .setFontColor(SECONDARY_COLOR)
                .setTextAlignment(TextAlignment.CENTER));
    }

    private Cell createInfoCell(String label, String value, PdfFont boldFont, PdfFont regularFont) {
        return new Cell()
                .setBorder(Border.NO_BORDER)
                .setBackgroundColor(LIGHT_GRAY)
                .setPadding(10)
                .add(new Paragraph(label)
                        .setFont(boldFont)
                        .setFontSize(10)
                        .setFontColor(SECONDARY_COLOR)
                        .setMarginBottom(3))
                .add(new Paragraph(value)
                        .setFont(regularFont)
                        .setFontSize(11));
    }

    private Cell createTableCell(String text, PdfFont font, TextAlignment alignment, DeviceRgb bgColor) {
        return new Cell()
                .add(new Paragraph(text)
                        .setFont(font)
                        .setFontSize(9))
                .setTextAlignment(alignment)
                .setBackgroundColor(bgColor)
                .setPadding(8)
                .setBorder(new SolidBorder(BORDER_COLOR, 0.5f));
    }

    private String formatCurrency(BigDecimal amount) {
        return CURRENCY_FORMATTER.format(amount);
    }

    private String getStatutColor(CommandeFournisseur.Statut statut) {
        switch (statut) {
            case EN_ATTENTE:
                return "#F59E0B"; // Orange
            case RECUE:
                return "#10B981"; // Green
            case ANNULEE:
                return "#EF4444"; // Red
            default:
                return "#6B7280"; // Gray
        }
    }
}