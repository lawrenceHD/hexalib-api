package com.hexalib.api.vente.service;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
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
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class FactureService {

    private final VenteRepository venteRepository;

    private static final String hexalib_NOM = "HEXALIB";
    private static final String hexalib_ADRESSE = "123 Rue du Livre";
    private static final String hexalib_VILLE = "Yaoundé, Cameroun";
    private static final String hexalib_TEL = "Tél: +237 123 456 789";

    /**
     * Générer une facture PDF
     */
    public byte[] genererFacturePDF(UUID venteId) {
        log.info("Génération de la facture PDF pour la vente: {}", venteId);

        Vente vente = venteRepository.findById(venteId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvée"));

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(baos);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc);

            // En-tête
            ajouterEntete(document, vente);

            // Ligne de séparation
            document.add(new Paragraph("\n"));

            // Tableau des articles
            ajouterTableauArticles(document, vente);

            // Totaux
            ajouterTotaux(document, vente);

            // Pied de page
            ajouterPiedPage(document);

            document.close();

            log.info("Facture PDF générée avec succès");
            return baos.toByteArray();

        } catch (Exception e) {
            log.error("Erreur lors de la génération du PDF", e);
            throw new RuntimeException("Erreur lors de la génération de la facture PDF", e);
        }
    }

    /**
     * Ajouter l'en-tête de la facture
     */
    private void ajouterEntete(Document document, Vente vente) {
        // Nom de la hexalib
        Paragraph titre = new Paragraph(hexalib_NOM)
                .setFontSize(20)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(titre);

        // Coordonnées
        Paragraph coordonnees = new Paragraph(
                hexalib_ADRESSE + "\n" +
                hexalib_VILLE + "\n" +
                hexalib_TEL
        )
                .setFontSize(10)
                .setTextAlignment(TextAlignment.CENTER);
        document.add(coordonnees);

        // Ligne de séparation
        document.add(new Paragraph("=".repeat(80))
                .setTextAlignment(TextAlignment.CENTER));

        // Informations facture
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        
        Table infoTable = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .useAllAvailableWidth();
        
        infoTable.addCell(createCell("Facture N° : " + vente.getNumeroFacture(), false));
        infoTable.addCell(createCell("Date : " + vente.getDateVente().format(formatter), false));
        infoTable.addCell(createCell("Vendeur : " + vente.getVendeur().getNomComplet(), false));
        infoTable.addCell(createCell("Statut : " + vente.getStatut(), false));

        document.add(infoTable);
    }

    /**
     * Ajouter le tableau des articles
     */
    private void ajouterTableauArticles(Document document, Vente vente) {
        // En-têtes du tableau
        float[] columnWidths = {3, 7, 2, 3, 3, 3};
        Table table = new Table(UnitValue.createPercentArray(columnWidths))
                .useAllAvailableWidth();

        // Headers
        table.addHeaderCell(createHeaderCell("CODE"));
        table.addHeaderCell(createHeaderCell("TITRE"));
        table.addHeaderCell(createHeaderCell("QTÉ"));
        table.addHeaderCell(createHeaderCell("P.U"));
        table.addHeaderCell(createHeaderCell("RÉDUC."));
        table.addHeaderCell(createHeaderCell("TOTAL"));

        // Lignes
        for (LigneVente ligne : vente.getLignes()) {
            table.addCell(createCell(ligne.getCodeLivre(), true));
            table.addCell(createCell(ligne.getTitreLivre(), true));
            table.addCell(createCell(String.valueOf(ligne.getQuantite()), true));
            table.addCell(createCell(formatMontant(ligne.getPrixUnitaire()), true));
            table.addCell(createCell(formatMontant(ligne.getMontantReduction()), true));
            table.addCell(createCell(formatMontant(ligne.getSousTotal()), true));
        }

        document.add(table);
    }

    /**
     * Ajouter les totaux
     */
    private void ajouterTotaux(Document document, Vente vente) {
        document.add(new Paragraph("\n"));

        Table totauxTable = new Table(UnitValue.createPercentArray(new float[]{3, 1}))
                .useAllAvailableWidth()
                .setTextAlignment(TextAlignment.RIGHT);

        totauxTable.addCell(createCell("Sous-total :", false));
        totauxTable.addCell(createCell(formatMontant(vente.getMontantHT()) + " XAF", false));

        totauxTable.addCell(createCell("Total des réductions :", false));
        totauxTable.addCell(createCell("-" + formatMontant(vente.getMontantReductions()) + " XAF", false));

        // Ligne de séparation
        totauxTable.addCell(new Cell(1, 2)
                .setBorder(Border.NO_BORDER)
                .add(new Paragraph("—".repeat(60)).setTextAlignment(TextAlignment.RIGHT)));

        totauxTable.addCell(createCell("TOTAL À PAYER :", false).setBold());
        totauxTable.addCell(createCell(formatMontant(vente.getMontantTTC()) + " XAF", false)
                .setBold()
                .setFontSize(14));

        document.add(totauxTable);
    }

    /**
     * Ajouter le pied de page
     */
    private void ajouterPiedPage(Document document) {
        document.add(new Paragraph("\n\n"));
        
        Paragraph separator = new Paragraph("=".repeat(80))
                .setTextAlignment(TextAlignment.CENTER);
        document.add(separator);

        Paragraph merci = new Paragraph("Merci de votre visite !")
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(merci);

        Paragraph mentions = new Paragraph("Facture non soumise à TVA - Article 293 B du CGI")
                .setFontSize(8)
                .setItalic()
                .setTextAlignment(TextAlignment.CENTER);
        document.add(mentions);
    }

    /**
     * Créer une cellule d'en-tête
     */
    private Cell createHeaderCell(String text) {
        return new Cell()
                .add(new Paragraph(text).setBold())
                .setBackgroundColor(new DeviceRgb(37, 99, 235))
                .setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER)
                .setPadding(5);
    }

    /**
     * Créer une cellule normale
     */
    private Cell createCell(String text, boolean centerAlign) {
        Cell cell = new Cell()
                .add(new Paragraph(text))
                .setPadding(5);
        
        if (centerAlign) {
            cell.setTextAlignment(TextAlignment.CENTER);
        }
        
        return cell;
    }

    /**
     * Formater un montant
     */
    private String formatMontant(BigDecimal montant) {
        if (montant == null) {
            return "0";
        }
        return String.format("%,.0f", montant.doubleValue());
    }
}