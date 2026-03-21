package com.hexalib.api.comptabilite.controller;
 
import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.comptabilite.dto.CategorieDepenseRequest;
import com.hexalib.api.comptabilite.dto.CategorieDepenseResponse;
import com.hexalib.api.comptabilite.service.CategorieDepenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.util.List;
 
@RestController
@RequestMapping("/api/comptabilite/categories-depenses")
@RequiredArgsConstructor
@Tag(name = "Comptabilité - Catégories Dépenses")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CategorieDepenseController {
 
    private final CategorieDepenseService service;
 
    @PostMapping
    @Operation(summary = "Créer une catégorie de dépense")
    public ResponseEntity<ApiResponse<CategorieDepenseResponse>> create(
            @Valid @RequestBody CategorieDepenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Catégorie créée", service.create(request)));
    }
 
    @GetMapping
    @Operation(summary = "Lister toutes les catégories")
    public ResponseEntity<ApiResponse<List<CategorieDepenseResponse>>> getAll() {
        return ResponseEntity.ok(ApiResponse.success(service.getAll()));
    }
 
    @GetMapping("/actives")
    @Operation(summary = "Lister les catégories actives")
    public ResponseEntity<ApiResponse<List<CategorieDepenseResponse>>> getAllActives() {
        return ResponseEntity.ok(ApiResponse.success(service.getAllActives()));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une catégorie")
    public ResponseEntity<ApiResponse<CategorieDepenseResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody CategorieDepenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Catégorie mise à jour", service.update(id, request)));
    }
 
    @PatchMapping("/{id}/toggle-statut")
    @Operation(summary = "Activer / Désactiver une catégorie")
    public ResponseEntity<ApiResponse<CategorieDepenseResponse>> toggleStatut(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success("Statut modifié", service.toggleStatut(id)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une catégorie")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Catégorie supprimée", null));
    }
}
 