package com.hexalib.api.livre.controller;

import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.livre.dto.ImportJobDTO;
import com.hexalib.api.livre.service.LivreImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/livres/import")
@RequiredArgsConstructor
@Tag(name = "Import Livres", description = "Import Excel avec progression par batch")
public class LivreImportController {

    private final LivreImportService importService;

    /**
     * ÉTAPE 1 — Upload du fichier Excel.
     * Parse toutes les lignes, crée le job en mémoire.
     * Retourne le jobId et le nombre total de batches.
     *
     * POST /api/livres/import/upload
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Uploader un fichier Excel",
               description = "Parse le fichier et crée un job d'import. Retourne le jobId.")
    public ResponseEntity<ApiResponse<ImportJobDTO.ImportJobStarted>> upload(
            @RequestParam("file") MultipartFile file) {
        try {
            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Fichier vide"));
            }
            String filename = file.getOriginalFilename();
            if (filename == null ||
                    (!filename.toLowerCase().endsWith(".xlsx") &&
                     !filename.toLowerCase().endsWith(".xls"))) {
                return ResponseEntity.badRequest()
                        .body(ApiResponse.error("Format invalide — utilisez un fichier .xlsx"));
            }

            ImportJobDTO.ImportJobStarted result = importService.parseEtCreerJob(file);
            return ResponseEntity.ok(
                    ApiResponse.success("Fichier parsé — " + result.getTotalLignes() + " lignes détectées", result));

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur lors du parsing : " + e.getMessage()));
        }
    }

    /**
     * ÉTAPE 2 — Traiter un batch.
     * Le frontend appelle cet endpoint répétitivement (batch 1, 2, 3…)
     * jusqu'à ce que termine = true dans la réponse.
     *
     * POST /api/livres/import/{jobId}/batch/{batchNumero}
     */
    @PostMapping("/{jobId}/batch/{batchNumero}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Traiter un batch d'import",
               description = "Appeler séquentiellement batch/1, batch/2… jusqu'à termine=true")
    public ResponseEntity<ApiResponse<ImportJobDTO.ImportBatchResult>> traiterBatch(
            @PathVariable String jobId,
            @PathVariable int batchNumero) {
        try {
            ImportJobDTO.ImportBatchResult result = importService.traiterBatch(jobId, batchNumero);
            return ResponseEntity.ok(ApiResponse.success("Batch " + batchNumero + " traité", result));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error(e.getMessage()));
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body(ApiResponse.error("Erreur batch : " + e.getMessage()));
        }
    }
}