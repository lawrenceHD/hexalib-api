package com.hexalib.api.stock.controller;

import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.stock.dto.MouvementStockRequest;
import com.hexalib.api.stock.dto.MouvementStockResponse;
import com.hexalib.api.stock.model.TypeMouvement;
import com.hexalib.api.stock.service.MouvementStockService;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/mouvements")
@RequiredArgsConstructor
@Tag(name = "Mouvements Stock", description = "Gestion des mouvements de stock")
@SecurityRequirement(name = "bearerAuth")
public class MouvementStockController {

    private final MouvementStockService mouvementStockService;

    /**
     * Créer un mouvement de stock manuel (Admin uniquement)
     */
    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un mouvement", description = "Enregistrer un mouvement de stock manuel (Admin)")
    public ResponseEntity<ApiResponse<MouvementStockResponse>> create(
            @Valid @RequestBody MouvementStockRequest request) {
        
        MouvementStockResponse response = mouvementStockService.create(request);
        
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Mouvement de stock créé avec succès", response));
    }

    /**
     * Récupérer tous les mouvements
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Lister les mouvements", description = "Récupérer tous les mouvements de stock (Admin)")
    public ResponseEntity<ApiResponse<Page<MouvementStockResponse>>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateMouvement").descending());
        Page<MouvementStockResponse> mouvements = mouvementStockService.getAll(pageable);
        
        return ResponseEntity.ok(ApiResponse.success(mouvements));
    }

    /**
     * Récupérer les mouvements d'un livre
     */
    @GetMapping("/livre/{livreId}")
    @Operation(summary = "Mouvements d'un livre", description = "Récupérer l'historique des mouvements d'un livre")
    public ResponseEntity<ApiResponse<Page<MouvementStockResponse>>> getByLivre(
            @PathVariable String livreId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MouvementStockResponse> mouvements = 
                mouvementStockService.getByLivre(UUID.fromString(livreId), pageable);
        
        return ResponseEntity.ok(ApiResponse.success(mouvements));
    }

    /**
     * Récupérer l'historique complet d'un livre
     */
    @GetMapping("/livre/{livreId}/historique")
    @Operation(summary = "Historique complet", description = "Récupérer l'historique complet d'un livre")
    public ResponseEntity<ApiResponse<List<MouvementStockResponse>>> getHistoriqueLivre(
            @PathVariable String livreId) {
        
        List<MouvementStockResponse> mouvements = 
                mouvementStockService.getHistoriqueLivre(UUID.fromString(livreId));
        
        return ResponseEntity.ok(ApiResponse.success(mouvements));
    }

    /**
     * Récupérer les mouvements par type
     */
    @GetMapping("/type/{type}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Mouvements par type", description = "Récupérer les mouvements par type (Admin)")
    public ResponseEntity<ApiResponse<Page<MouvementStockResponse>>> getByType(
            @PathVariable TypeMouvement type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MouvementStockResponse> mouvements = mouvementStockService.getByType(type, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(mouvements));
    }

    /**
     * Recherche avec filtres multiples
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rechercher avec filtres", description = "Rechercher des mouvements avec filtres (Admin)")
    public ResponseEntity<ApiResponse<Page<MouvementStockResponse>>> search(
            @RequestParam(required = false) String livreId,
            @RequestParam(required = false) TypeMouvement type,
            @RequestParam(required = false) String userId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fin,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        Pageable pageable = PageRequest.of(page, size);
        Page<MouvementStockResponse> mouvements = 
                mouvementStockService.getWithFilters(livreId, type, userId, debut, fin, pageable);
        
        return ResponseEntity.ok(ApiResponse.success(mouvements));
    }
}
