package com.hexalib.api.vente.controller;

import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.vente.dto.GlobalStatsResponse;
import com.hexalib.api.vente.dto.VendeurStatsResponse;
import com.hexalib.api.vente.dto.VenteRequest;
import com.hexalib.api.vente.dto.VenteResponse;
import com.hexalib.api.vente.service.FactureService;
import com.hexalib.api.vente.service.VenteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/ventes")
@RequiredArgsConstructor
@Tag(name = "Ventes", description = "Gestion des ventes et point de vente")
@SecurityRequirement(name = "bearerAuth")
public class VenteController {

    private final VenteService venteService;
    private final FactureService factureService;

    /**
     * Créer une nouvelle vente
     */
    @PostMapping
    @Operation(summary = "Créer une vente", description = "Enregistrer une nouvelle vente avec déduction automatique du stock")
    public ResponseEntity<ApiResponse<VenteResponse>> create(
            @Valid @RequestBody VenteRequest request) {
        
        VenteResponse response = venteService.create(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Vente créée avec succès", response));
    }

    /**
     * Récupérer toutes les ventes (paginées)
     */
    @GetMapping
    @Operation(summary = "Lister les ventes", description = "Récupérer toutes les ventes avec pagination")
    public ResponseEntity<ApiResponse<Page<VenteResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateVente").descending());
        Page<VenteResponse> ventes = venteService.getAll(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(ventes));
    }

    /**
     * Récupérer une vente par ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une vente", description = "Récupérer les détails complets d'une vente")
    public ResponseEntity<ApiResponse<VenteResponse>> getById(@PathVariable String id) {
        VenteResponse response = venteService.getById(UUID.fromString(id));
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Rechercher des ventes
     */
    @GetMapping("/search")
    @Operation(summary = "Rechercher des ventes", description = "Rechercher par numéro de facture ou nom du vendeur")
    public ResponseEntity<ApiResponse<Page<VenteResponse>>> search(
            @RequestParam String q,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VenteResponse> ventes = venteService.search(q, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(ventes));
    }

    /**
     * Récupérer les ventes d'un vendeur
     */
    @GetMapping("/vendeur/{vendeurId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ventes d'un vendeur", description = "Récupérer toutes les ventes d'un vendeur (Admin)")
    public ResponseEntity<ApiResponse<Page<VenteResponse>>> getByVendeur(
            @PathVariable String vendeurId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VenteResponse> ventes = venteService.getByVendeur(vendeurId, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(ventes));
    }

    /**
     * Récupérer les ventes du vendeur connecté
     */
    @GetMapping("/mes-ventes")
    @Operation(summary = "Mes ventes", description = "Récupérer les ventes du vendeur connecté")
    public ResponseEntity<ApiResponse<Page<VenteResponse>>> getMesVentes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<VenteResponse> ventes = venteService.getMesVentes(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(ventes));
    }

    /**
     * Annuler une vente (Admin uniquement)
     */
    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Annuler une vente", description = "Annuler une vente et restaurer le stock (Admin)")
    public ResponseEntity<ApiResponse<VenteResponse>> annuler(
            @PathVariable String id,
            @RequestParam String motif) {
        
        VenteResponse response = venteService.annuler(UUID.fromString(id), motif);
        
        return ResponseEntity.ok(ApiResponse.success("Vente annulée avec succès", response));
    }

    /**
     * Générer et télécharger la facture PDF
     */
    @GetMapping("/{id}/facture")
    @Operation(summary = "Télécharger facture PDF", description = "Générer et télécharger la facture au format PDF")
    public ResponseEntity<byte[]> getFacturePDF(@PathVariable String id) {
        byte[] pdfBytes = factureService.genererFacturePDF(UUID.fromString(id));
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", "facture-" + id + ".pdf");
        
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdfBytes);
    }

    /**
     * Statistiques d'un vendeur
     */
    @GetMapping("/stats/vendeur/{vendeurId}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques vendeur", description = "Récupérer les statistiques d'un vendeur (Admin)")
    public ResponseEntity<ApiResponse<VendeurStatsResponse>> getStatsVendeur(
            @PathVariable String vendeurId) {
        
        VendeurStatsResponse stats = venteService.getStatsVendeur(vendeurId);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Statistiques du jour (global)
     */
    @GetMapping("/stats/jour")
    @Operation(summary = "Statistiques du jour", description = "Récupérer les statistiques globales du jour")
    public ResponseEntity<ApiResponse<GlobalStatsResponse>> getStatsJour(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        GlobalStatsResponse stats = venteService.getStatsJour(targetDate);
        
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}