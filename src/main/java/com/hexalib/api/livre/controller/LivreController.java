package com.hexalib.api.livre.controller;

import com.hexalib.api.livre.dto.ImportJobDTO;
import com.hexalib.api.livre.service.LivreExportService;
import com.hexalib.api.livre.service.LivreImportService;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpHeaders;

import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.livre.dto.LivreRequest;
import com.hexalib.api.livre.dto.LivreResponse;
import com.hexalib.api.livre.service.LivreService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/livres")
@RequiredArgsConstructor
@Tag(name = "Livres", description = "Gestion du catalogue de livres")
@SecurityRequirement(name = "bearerAuth")
public class LivreController {

    private final LivreService        livreService;
    private final LivreImportService  livreImportService;
    private final LivreExportService  livreExportService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un livre", description = "Ajouter un nouveau livre au catalogue (Admin uniquement)")
    public ResponseEntity<ApiResponse<LivreResponse>> createLivre(
            @Valid @RequestBody LivreRequest request) {
        LivreResponse livre = livreService.createLivre(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Livre créé avec succès", livre));
    }

    @GetMapping
    @Operation(summary = "Lister les livres", description = "Récupérer tous les livres avec pagination et filtres")
    public ResponseEntity<ApiResponse<PageResponse<LivreResponse>>> getAllLivres(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String categorieId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) String langue) {
        PageResponse<LivreResponse> livres = livreService.getAllLivres(
            page, size, search, categorieId, statut, langue
        );
        return ResponseEntity.ok(ApiResponse.success(livres));
    }

    @GetMapping("/stock-critique")
    @Operation(summary = "Livres en stock critique")
    public ResponseEntity<ApiResponse<List<LivreResponse>>> getLivresStockCritique() {
        List<LivreResponse> livres = livreService.getLivresStockCritique();
        return ResponseEntity.ok(ApiResponse.success(livres));
    }

    @GetMapping("/rupture")
    @Operation(summary = "Livres en rupture")
    public ResponseEntity<ApiResponse<List<LivreResponse>>> getLivresEnRupture() {
        List<LivreResponse> livres = livreService.getLivresEnRupture();
        return ResponseEntity.ok(ApiResponse.success(livres));
    }

    @GetMapping("/langues")
    @Operation(summary = "Liste des langues")
    public ResponseEntity<ApiResponse<List<String>>> getAllLangues() {
        List<String> langues = livreService.getAllLangues();
        return ResponseEntity.ok(ApiResponse.success(langues));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un livre")
    public ResponseEntity<ApiResponse<LivreResponse>> getLivreById(@PathVariable String id) {
        LivreResponse livre = livreService.getLivreById(id);
        return ResponseEntity.ok(ApiResponse.success(livre));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un livre")
    public ResponseEntity<ApiResponse<LivreResponse>> updateLivre(
            @PathVariable String id,
            @Valid @RequestBody LivreRequest request) {
        LivreResponse livre = livreService.updateLivre(id, request);
        return ResponseEntity.ok(ApiResponse.success("Livre modifié avec succès", livre));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un livre")
    public ResponseEntity<ApiResponse<Void>> deleteLivre(@PathVariable String id) {
        livreService.deleteLivre(id);
        return ResponseEntity.ok(ApiResponse.success("Livre supprimé avec succès", null));
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver un livre")
    public ResponseEntity<ApiResponse<LivreResponse>> toggleStatut(@PathVariable String id) {
        LivreResponse livre = livreService.toggleStatut(id);
        return ResponseEntity.ok(ApiResponse.success("Statut modifié avec succès", livre));
    }

    @PatchMapping("/{id}/ajuster-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajuster le stock")
    public ResponseEntity<ApiResponse<LivreResponse>> ajusterStock(
            @PathVariable String id,
            @RequestParam Integer quantite,
            @RequestParam(required = false) String motif) {
        LivreResponse livre = livreService.ajusterStock(id, quantite, motif);
        return ResponseEntity.ok(ApiResponse.success("Stock ajusté avec succès", livre));
    }

    // ══════════════════════════════════════════════════════════════════
    // IMPORT EXCEL — 2 étapes avec progression
    // Les routes /import/upload et /import/{jobId}/batch/{n}
    // sont gérées par LivreImportController (@RequestMapping("/api/livres/import"))
    // Ces méthodes ci-dessous ont été supprimées de ce controller pour éviter
    // les conflits de routes. Utilisez LivreImportController à la place.
    // ══════════════════════════════════════════════════════════════════

    // ══════════════════════════════════════════════════════════════════
    // EXPORT EXCEL
    // ══════════════════════════════════════════════════════════════════

    @GetMapping("/export")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Exporter l'inventaire", description = "Exporter tous les livres au format Excel (Admin uniquement)")
    public ResponseEntity<byte[]> exportLivres() {
        byte[] excelBytes = livreExportService.exportLivres();

        String filename = "Inventaire_Hexalib_" +
                java.time.LocalDate.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd")) + ".xlsx";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType(
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"));
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(excelBytes, headers, HttpStatus.OK);
    }
}