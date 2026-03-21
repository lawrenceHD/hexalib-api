package com.hexalib.api.comptabilite.controller;
 
import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.comptabilite.dto.DepenseRequest;
import com.hexalib.api.comptabilite.dto.DepenseResponse;
import com.hexalib.api.comptabilite.service.DepenseService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDate;
 
@RestController
@RequestMapping("/api/comptabilite/depenses")
@RequiredArgsConstructor
@Tag(name = "Comptabilité - Dépenses")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class DepenseController {
 
    private final DepenseService service;
 
    @PostMapping
    @Operation(summary = "Enregistrer une dépense")
    public ResponseEntity<ApiResponse<DepenseResponse>> create(
            @Valid @RequestBody DepenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dépense enregistrée", service.create(request)));
    }
 
    @GetMapping
    @Operation(summary = "Lister les dépenses avec filtres")
    public ResponseEntity<ApiResponse<PageResponse<DepenseResponse>>> getAll(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false)    String categorieId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        return ResponseEntity.ok(ApiResponse.success(service.getAll(page, size, categorieId, debut, fin)));
    }
 
    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une dépense")
    public ResponseEntity<ApiResponse<DepenseResponse>> getById(@PathVariable String id) {
        return ResponseEntity.ok(ApiResponse.success(service.getById(id)));
    }
 
    @PutMapping("/{id}")
    @Operation(summary = "Modifier une dépense")
    public ResponseEntity<ApiResponse<DepenseResponse>> update(
            @PathVariable String id,
            @Valid @RequestBody DepenseRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Dépense mise à jour", service.update(id, request)));
    }
 
    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une dépense")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable String id) {
        service.delete(id);
        return ResponseEntity.ok(ApiResponse.success("Dépense supprimée", null));
    }
}