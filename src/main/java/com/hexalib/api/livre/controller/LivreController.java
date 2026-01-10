package com.hexalib.api.livre.controller;

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

    private final LivreService livreService;

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
    @Operation(summary = "Livres en stock critique", description = "Récupérer tous les livres dont le stock est inférieur ou égal au seuil minimal")
    public ResponseEntity<ApiResponse<List<LivreResponse>>> getLivresStockCritique() {
        List<LivreResponse> livres = livreService.getLivresStockCritique();
        return ResponseEntity.ok(ApiResponse.success(livres));
    }

    @GetMapping("/rupture")
    @Operation(summary = "Livres en rupture", description = "Récupérer tous les livres en rupture de stock")
    public ResponseEntity<ApiResponse<List<LivreResponse>>> getLivresEnRupture() {
        List<LivreResponse> livres = livreService.getLivresEnRupture();
        return ResponseEntity.ok(ApiResponse.success(livres));
    }

    @GetMapping("/langues")
    @Operation(summary = "Liste des langues", description = "Récupérer toutes les langues disponibles dans le catalogue")
    public ResponseEntity<ApiResponse<List<String>>> getAllLangues() {
        List<String> langues = livreService.getAllLangues();
        return ResponseEntity.ok(ApiResponse.success(langues));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'un livre", description = "Récupérer les détails complets d'un livre par son ID")
    public ResponseEntity<ApiResponse<LivreResponse>> getLivreById(@PathVariable String id) {
        LivreResponse livre = livreService.getLivreById(id);
        return ResponseEntity.ok(ApiResponse.success(livre));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un livre", description = "Modifier les informations d'un livre existant (Admin uniquement)")
    public ResponseEntity<ApiResponse<LivreResponse>> updateLivre(
            @PathVariable String id,
            @Valid @RequestBody LivreRequest request) {
        LivreResponse livre = livreService.updateLivre(id, request);
        return ResponseEntity.ok(ApiResponse.success("Livre modifié avec succès", livre));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un livre", description = "Supprimer un livre du catalogue (Admin uniquement)")
    public ResponseEntity<ApiResponse<Void>> deleteLivre(@PathVariable String id) {
        livreService.deleteLivre(id);
        return ResponseEntity.ok(ApiResponse.success("Livre supprimé avec succès", null));
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer/Désactiver un livre", description = "Changer le statut d'un livre (Admin uniquement)")
    public ResponseEntity<ApiResponse<LivreResponse>> toggleStatut(@PathVariable String id) {
        LivreResponse livre = livreService.toggleStatut(id);
        return ResponseEntity.ok(ApiResponse.success("Statut modifié avec succès", livre));
    }

    @PatchMapping("/{id}/ajuster-stock")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Ajuster le stock", description = "Modifier manuellement la quantité en stock d'un livre (Admin uniquement)")
    public ResponseEntity<ApiResponse<LivreResponse>> ajusterStock(
            @PathVariable String id,
            @RequestParam Integer quantite,
            @RequestParam(required = false) String motif) {
        LivreResponse livre = livreService.ajusterStock(id, quantite, motif);
        return ResponseEntity.ok(ApiResponse.success("Stock ajusté avec succès", livre));
    }
}