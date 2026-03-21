package com.hexalib.api.comptabilite.controller;
 
import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.comptabilite.dto.*;
import com.hexalib.api.comptabilite.service.ComptabiliteService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
 
import java.time.LocalDate;
 
@RestController
@RequestMapping("/api/comptabilite")
@RequiredArgsConstructor
@Tag(name = "Comptabilité - Rapports")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class ComptabiliteController {
 
    private final ComptabiliteService service;
 
    // ── Dashboard ──────────────────────────────────────────────────
 
    @GetMapping("/dashboard")
    @Operation(summary = "Tableau de bord comptable")
    public ResponseEntity<ApiResponse<DashboardComptaDTO>> getDashboard(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate d = debut != null ? debut : LocalDate.now().withDayOfMonth(1);
        LocalDate f = fin   != null ? fin   : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(service.getDashboard(d, f)));
    }
 
    // ── Rapports Ventes ────────────────────────────────────────────
 
    @GetMapping("/rapports/ventes")
    @Operation(summary = "Rapport ventes",
               description = "typeRapport: AVEC_REDUCTION | SANS_REDUCTION | COMBINE")
    public ResponseEntity<ApiResponse<RapportVentesDTO>> getRapportVentes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin,
            @RequestParam(defaultValue = "COMBINE") String typeRapport) {
        LocalDate d = debut != null ? debut : LocalDate.now().withDayOfMonth(1);
        LocalDate f = fin   != null ? fin   : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(service.getRapportVentes(d, f, typeRapport)));
    }
 
    // ── Rapports Financiers ────────────────────────────────────────
 
    @GetMapping("/rapports/compte-resultat")
    @Operation(summary = "Compte de résultat")
    public ResponseEntity<ApiResponse<RapportCompteResultatDTO>> getCompteResultat(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate d = debut != null ? debut : LocalDate.now().withDayOfMonth(1);
        LocalDate f = fin   != null ? fin   : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(service.getRapportCompteResultat(d, f)));
    }
 
    @GetMapping("/rapports/tresorerie")
    @Operation(summary = "Rapport de trésorerie")
    public ResponseEntity<ApiResponse<RapportTresorerieDTO>> getTresorerie(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate debut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fin) {
        LocalDate d = debut != null ? debut : LocalDate.now().withDayOfMonth(1);
        LocalDate f = fin   != null ? fin   : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(service.getRapportTresorerie(d, f)));
    }
 
    @GetMapping("/rapports/stock-valorise")
    @Operation(summary = "Rapport stock valorisé")
    public ResponseEntity<ApiResponse<RapportStockValoriseDTO>> getStockValorise() {
        return ResponseEntity.ok(ApiResponse.success(service.getRapportStockValorise()));
    }
}
 