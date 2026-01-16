package com.hexalib.api.reduction.controller;

import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.reduction.dto.ReductionRequest;
import com.hexalib.api.reduction.dto.ReductionResponse;
import com.hexalib.api.reduction.service.ReductionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/reductions")
@RequiredArgsConstructor
@Tag(name = "Réductions", description = "Gestion des réductions et promotions")
@SecurityRequirement(name = "bearerAuth")
public class ReductionController {

    private final ReductionService reductionService;

    /**
     * Créer une nouvelle réduction (Admin uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une réduction", description = "Ajouter une nouvelle réduction (Admin uniquement)")
    public ResponseEntity<ApiResponse<ReductionResponse>> create(
            @Valid @RequestBody ReductionRequest request) {
        
        ReductionResponse response = reductionService.create(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Réduction créée avec succès", response));
    }

    /**
     * Récupérer toutes les réductions (paginées)
     */
    @GetMapping
    @Operation(summary = "Lister les réductions", description = "Récupérer toutes les réductions avec pagination")
    public ResponseEntity<ApiResponse<Page<ReductionResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<ReductionResponse> reductions = reductionService.getAll(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(reductions));
    }

    /**
     * Récupérer une réduction par ID
     */
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une réduction", description = "Récupérer les détails d'une réduction par son ID")
    public ResponseEntity<ApiResponse<ReductionResponse>> getById(@PathVariable UUID id) {
        ReductionResponse response = reductionService.getById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    /**
     * Rechercher des réductions par intitulé
     */
    @GetMapping("/search")
    @Operation(summary = "Rechercher des réductions", description = "Rechercher des réductions par intitulé")
    public ResponseEntity<ApiResponse<Page<ReductionResponse>>> search(
            @RequestParam String intitule,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReductionResponse> reductions = reductionService.searchByIntitule(intitule, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(reductions));
    }

    /**
     * Récupérer les réductions actives
     */
    @GetMapping("/actives")
    @Operation(summary = "Réductions actives", description = "Récupérer toutes les réductions actives")
    public ResponseEntity<ApiResponse<Page<ReductionResponse>>> getActives(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateDebut").descending());
        Page<ReductionResponse> reductions = reductionService.getActives(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(reductions));
    }

    /**
     * Récupérer les réductions valides (actives + période en cours)
     */
    @GetMapping("/valides")
    @Operation(summary = "Réductions valides", description = "Récupérer les réductions valides (actives et dans la période)")
    public ResponseEntity<ApiResponse<List<ReductionResponse>>> getValidReductions() {
        List<ReductionResponse> reductions = reductionService.getValidReductions();
        return ResponseEntity.ok(ApiResponse.success(reductions));
    }

    /**
     * Récupérer les réductions expirées
     */
    @GetMapping("/expirees")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réductions expirées", description = "Récupérer toutes les réductions expirées (Admin uniquement)")
    public ResponseEntity<ApiResponse<Page<ReductionResponse>>> getExpired(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<ReductionResponse> reductions = reductionService.getExpired(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(reductions));
    }

    /**
     * Récupérer les réductions applicables à un livre
     */
    @GetMapping("/livre/{livreId}")
    @Operation(summary = "Réductions pour un livre", description = "Récupérer toutes les réductions applicables à un livre")
    public ResponseEntity<ApiResponse<List<ReductionResponse>>> getApplicableForLivre(
            @PathVariable UUID livreId) {
        
        List<ReductionResponse> reductions = 
                reductionService.getApplicableReductionsForLivre(livreId);
        
        return ResponseEntity.ok(ApiResponse.success(reductions));
    }

    /**
     * Récupérer la meilleure réduction pour un livre
     */
    @GetMapping("/livre/{livreId}/meilleure")
    @Operation(summary = "Meilleure réduction pour un livre", description = "Récupérer la meilleure réduction applicable à un livre")
    public ResponseEntity<ApiResponse<ReductionResponse>> getBestForLivre(
            @PathVariable UUID livreId) {
        
        ReductionResponse reduction = reductionService.getBestReductionForLivre(livreId);
        
        return ResponseEntity.ok(ApiResponse.success(reduction));
    }

    /**
     * Mettre à jour une réduction (Admin uniquement)
     */
    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une réduction", description = "Modifier les informations d'une réduction (Admin uniquement)")
    public ResponseEntity<ApiResponse<ReductionResponse>> update(
            @PathVariable UUID id,
            @Valid @RequestBody ReductionRequest request) {
        
        ReductionResponse response = reductionService.update(id, request);
        
        return ResponseEntity.ok(ApiResponse.success("Réduction mise à jour avec succès", response));
    }

    /**
     * Activer/Désactiver une réduction (Admin uniquement)
     */
    @PatchMapping("/{id}/toggle")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver une réduction", description = "Changer le statut actif d'une réduction (Admin uniquement)")
    public ResponseEntity<ApiResponse<ReductionResponse>> toggleActif(@PathVariable UUID id) {
        ReductionResponse response = reductionService.toggleActif(id);
        
        return ResponseEntity.ok(ApiResponse.success("Statut de la réduction modifié", response));
    }

    /**
     * Supprimer une réduction (Admin uniquement)
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une réduction", description = "Supprimer une réduction (Admin uniquement)")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        reductionService.delete(id);
        
        return ResponseEntity.ok(ApiResponse.success("Réduction supprimée avec succès", null));
    }
}