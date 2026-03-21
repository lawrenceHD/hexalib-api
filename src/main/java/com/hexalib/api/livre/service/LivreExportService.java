package com.hexalib.api.livre.service;

import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreExportService {

    private final LivreRepository livreRepository;

    public byte[] exportLivres() {
        log.info("Export inventaire livres");
        List<Livre> livres = livreRepository.findAll();

        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

            Sheet sheet = workbook.createSheet("Inventaire");

            // Styles
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);
            headerStyle.setBorderBottom(BorderStyle.THIN);

            CellStyle criticalStyle = workbook.createCellStyle();
            criticalStyle.setFillForegroundColor(IndexedColors.ROSE.getIndex());
            criticalStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            CellStyle normalStyle = workbook.createCellStyle();
            normalStyle.setBorderBottom(BorderStyle.THIN);
            normalStyle.setBorderLeft(BorderStyle.THIN);
            normalStyle.setBorderRight(BorderStyle.THIN);

            // ── Ligne 1 : Titre ──
            Row titleRow = sheet.createRow(0);
            Cell titleCell = titleRow.createCell(0);
            titleCell.setCellValue("INVENTAIRE COMPLET — HEXALIB — " +
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
            CellStyle titleStyle = workbook.createCellStyle();
            Font titleFont = workbook.createFont();
            titleFont.setBold(true);
            titleFont.setFontHeightInPoints((short) 13);
            titleStyle.setFont(titleFont);
            titleCell.setCellStyle(titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 12));
            titleRow.setHeightInPoints(25);

            // ── Ligne 2 : En-têtes ──
            String[] headers = {
                "CODE", "TITRE", "AUTEUR", "MAISON D'EDITION", "CATEGORIE",
                "LANGUE", "PRIX VENTE (XAF)", "PRIX ACHAT (XAF)", "STOCK",
                "SEUIL MIN.", "STATUT STOCK", "EMPLACEMENT", "STATUT"
            };
            int[] colWidths = {18, 40, 25, 30, 20, 12, 16, 16, 10, 10, 14, 18, 10};

            Row headerRow = sheet.createRow(1);
            headerRow.setHeightInPoints(20);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
                sheet.setColumnWidth(i, colWidths[i] * 256);
            }

            // ── Données ──
            int rowIdx = 2;
            for (Livre livre : livres) {
                Row row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(16);

                boolean stockCritique = livre.isStockCritique();

                setCell(row, 0,  livre.getCode(),                         workbook, stockCritique ? criticalStyle : normalStyle);
                setCell(row, 1,  livre.getTitre(),                        workbook, normalStyle);
                setCell(row, 2,  livre.getAuteur(),                       workbook, normalStyle);
                setCell(row, 3,  livre.getMaisonEdition(),                workbook, normalStyle);
                setCell(row, 4,  livre.getCategorie() != null ? livre.getCategorie().getNom() : "", workbook, normalStyle);
                setCell(row, 5,  livre.getLangue(),                       workbook, normalStyle);
                setCellNum(row, 6,  livre.getPrixVente() != null ? livre.getPrixVente().doubleValue() : 0, workbook, normalStyle);
                setCellNum(row, 7,  livre.getPrixAchat() != null ? livre.getPrixAchat().doubleValue() : 0, workbook, normalStyle);
                setCellNum(row, 8,  livre.getQuantiteStock(),             workbook, stockCritique ? criticalStyle : normalStyle);
                setCellNum(row, 9,  livre.getSeuilMinimal(),              workbook, normalStyle);
                setCell(row, 10, livre.getStatutStock().name(),           workbook, stockCritique ? criticalStyle : normalStyle);
                setCell(row, 11, livre.getEmplacement() != null ? livre.getEmplacement() : "", workbook, normalStyle);
                setCell(row, 12, livre.getStatut().name(),                workbook, normalStyle);
            }

            // Figer la ligne d'en-têtes
            sheet.createFreezePane(0, 2);
            sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, headers.length - 1));

            workbook.write(baos);
            log.info("Export terminé : {} livres exportés", livres.size());
            return baos.toByteArray();

        } catch (IOException e) {
            log.error("Erreur export Excel", e);
            throw new RuntimeException("Erreur lors de l'export Excel", e);
        }
    }

    private void setCell(Row row, int col, String value, Workbook wb, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value != null ? value : "");
        cell.setCellStyle(style);
    }

    private void setCellNum(Row row, int col, double value, Workbook wb, CellStyle style) {
        Cell cell = row.createCell(col);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }
}