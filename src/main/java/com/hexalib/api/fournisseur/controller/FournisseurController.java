package com.hexalib.api.fournisseur.controller;

import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.fournisseur.dto.FournisseurRequest;
import com.hexalib.api.fournisseur.dto.FournisseurResponse;
import com.hexalib.api.fournisseur.service.FournisseurService;
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
@RequestMapping("/api/fournisseurs")
@RequiredArgsConstructor
@Tag(name = "Fournisseurs", description = "Gestion des fournisseurs")
@SecurityRequirement(name = "bearerAuth")
public class FournisseurController {

    private final FournisseurService fournisseurService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un fournisseur", description = "Ajouter un nouveau fournisseur (Admin uniquement)")
    public ResponseEntity<ApiResponse<FournisseurResponse>> createFournisseur(
            @Valid @RequestBody FournisseurRequest request) {
        FournisseurResponse fournisseur = fournisseurService.createFournisseur(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Fournisseur créé avec succès", fournisseur));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les fournisseurs", description = "Récupérer tous les fournisseurs avec pagination et filtres (Admin uniquement)")
    public ResponseEntity<ApiResponse<PageResponse<FournisseurResponse>>> getAllFournisseurs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String statut) {
        PageResponse<FournisseurResponse> fournisseurs = fournisseurService.getAllFournisseurs(
            page, size, search, statut
        );
        return ResponseEntity.ok(ApiResponse.success(fournisseurs));
    }

    @GetMapping("/actifs")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les fournisseurs actifs", description = "Récupérer tous les fournisseurs actifs sans pagination (Admin uniquement)")
    public ResponseEntity<ApiResponse<List<FournisseurResponse>>> getAllFournisseursActifs() {
        List<FournisseurResponse> fournisseurs = fournisseurService.getAllFournisseursActifs();
        return ResponseEntity.ok(ApiResponse.success(fournisseurs));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Détail d'un fournisseur", description = "Récupérer les détails complets d'un fournisseur par son ID (Admin uniquement)")
    public ResponseEntity<ApiResponse<FournisseurResponse>> getFournisseurById(@PathVariable String id) {
        FournisseurResponse fournisseur = fournisseurService.getFournisseurById(id);
        return ResponseEntity.ok(ApiResponse.success(fournisseur));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un fournisseur", description = "Modifier les informations d'un fournisseur existant (Admin uniquement)")
    public ResponseEntity<ApiResponse<FournisseurResponse>> updateFournisseur(
            @PathVariable String id,
            @Valid @RequestBody FournisseurRequest request) {
        FournisseurResponse fournisseur = fournisseurService.updateFournisseur(id, request);
        return ResponseEntity.ok(ApiResponse.success("Fournisseur modifié avec succès", fournisseur));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un fournisseur", description = "Supprimer un fournisseur (Admin uniquement)")
    public ResponseEntity<ApiResponse<Void>> deleteFournisseur(@PathVariable String id) {
        fournisseurService.deleteFournisseur(id);
        return ResponseEntity.ok(ApiResponse.success("Fournisseur supprimé avec succès", null));
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver un fournisseur", description = "Changer le statut d'un fournisseur (Admin uniquement)")
    public ResponseEntity<ApiResponse<FournisseurResponse>> toggleStatut(@PathVariable String id) {
        FournisseurResponse fournisseur = fournisseurService.toggleStatut(id);
        return ResponseEntity.ok(ApiResponse.success("Statut modifié avec succès", fournisseur));
    }
}