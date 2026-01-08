package com.hexalib.api.categorie.controller;

import com.hexalib.api.categorie.dto.CategorieRequest;
import com.hexalib.api.categorie.dto.CategorieResponse;
import com.hexalib.api.categorie.service.CategorieService;
import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.common.dto.PageResponse;
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
@RequestMapping("/api/categories")
@RequiredArgsConstructor
@Tag(name = "Catégories", description = "Gestion des catégories de livres")
@SecurityRequirement(name = "bearerAuth")
public class CategorieController {

    private final CategorieService categorieService;

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer une catégorie", description = "Créer une nouvelle catégorie (Admin uniquement)")
    public ResponseEntity<ApiResponse<CategorieResponse>> createCategorie(
            @Valid @RequestBody CategorieRequest request) {
        CategorieResponse categorie = categorieService.createCategorie(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Catégorie créée avec succès", categorie));
    }

    @GetMapping
    @Operation(summary = "Lister les catégories", description = "Récupérer toutes les catégories avec pagination")
    public ResponseEntity<ApiResponse<PageResponse<CategorieResponse>>> getAllCategories(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search) {
        PageResponse<CategorieResponse> categories = categorieService.getAllCategories(page, size, search);
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/actives")
    @Operation(summary = "Lister les catégories actives", description = "Récupérer toutes les catégories actives sans pagination")
    public ResponseEntity<ApiResponse<List<CategorieResponse>>> getAllCategoriesActives() {
        List<CategorieResponse> categories = categorieService.getAllCategoriesActives();
        return ResponseEntity.ok(ApiResponse.success(categories));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une catégorie", description = "Récupérer les détails d'une catégorie par son ID")
    public ResponseEntity<ApiResponse<CategorieResponse>> getCategorieById(@PathVariable String id) {
        CategorieResponse categorie = categorieService.getCategorieById(id);
        return ResponseEntity.ok(ApiResponse.success(categorie));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier une catégorie", description = "Modifier une catégorie existante (Admin uniquement)")
    public ResponseEntity<ApiResponse<CategorieResponse>> updateCategorie(
            @PathVariable String id,
            @Valid @RequestBody CategorieRequest request) {
        CategorieResponse categorie = categorieService.updateCategorie(id, request);
        return ResponseEntity.ok(ApiResponse.success("Catégorie modifiée avec succès", categorie));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer une catégorie", description = "Supprimer une catégorie (Admin uniquement)")
    public ResponseEntity<ApiResponse<Void>> deleteCategorie(@PathVariable String id) {
        categorieService.deleteCategorie(id);
        return ResponseEntity.ok(ApiResponse.success("Catégorie supprimée avec succès", null));
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver une catégorie", description = "Changer le statut d'une catégorie (Admin uniquement)")
    public ResponseEntity<ApiResponse<CategorieResponse>> toggleStatut(@PathVariable String id) {
        CategorieResponse categorie = categorieService.toggleStatut(id);
        return ResponseEntity.ok(ApiResponse.success("Statut modifié avec succès", categorie));
    }
}