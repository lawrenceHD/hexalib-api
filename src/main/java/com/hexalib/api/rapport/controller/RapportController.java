package com.hexalib.api.rapport.controller;

import com.hexalib.api.auth.model.User;
import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.rapport.dto.DashboardAdminDTO;
import com.hexalib.api.rapport.dto.DashboardVendeurDTO;
import com.hexalib.api.rapport.dto.RapportJournalierDTO;
import com.hexalib.api.rapport.dto.RapportPeriodiqueDTO;
import com.hexalib.api.rapport.service.RapportPdfService;
import com.hexalib.api.rapport.service.RapportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Gestion des rapports et statistiques")
@SecurityRequirement(name = "bearerAuth")
public class RapportController {

    private final RapportService    rapportService;
    private final RapportPdfService rapportPdfService;

    // ==================== DASHBOARDS ====================

    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard Admin")
    public ResponseEntity<ApiResponse<DashboardAdminDTO>> getDashboardAdmin() {
        return ResponseEntity.ok(ApiResponse.success(rapportService.getDashboardAdmin()));
    }

    @GetMapping("/dashboard/vendeur")
    @PreAuthorize("hasAnyRole('ADMIN', 'VENDEUR')")
    @Operation(summary = "Dashboard Vendeur")
    public ResponseEntity<ApiResponse<DashboardVendeurDTO>> getDashboardVendeur(
            Authentication authentication) {
        String vendeurId = getVendeurId(authentication);
        return ResponseEntity.ok(ApiResponse.success(rapportService.getDashboardVendeur(vendeurId)));
    }

    // ==================== RAPPORT JOURNALIER ====================

    @GetMapping("/cloture-journaliere")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RapportJournalierDTO>> getRapportJournalier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(rapportService.getRapportJournalier(d)));
    }

    @GetMapping("/cloture-journaliere/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadRapportJournalierPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return createPdfResponse(
            rapportPdfService.genererRapportJournalierPDF(rapportService.getRapportJournalier(d)),
            "Rapport_Journalier_" + d + ".pdf");
    }

    // ==================== RAPPORTS PÉRIODIQUES ====================

    @GetMapping("/hebdomadaire")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportHebdomadaire(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        LocalDate d = dateFin != null ? dateFin : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(rapportService.getRapportHebdomadaire(d)));
    }

    @GetMapping("/hebdomadaire/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadRapportHebdomadairePDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        LocalDate d = dateFin != null ? dateFin : LocalDate.now();
        return createPdfResponse(
            rapportPdfService.genererRapportPeriodiquePDF(rapportService.getRapportHebdomadaire(d)),
            "Rapport_Hebdomadaire_" + d + ".pdf");
    }

    @GetMapping("/mensuel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportMensuel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.success(rapportService.getRapportMensuel(d)));
    }

    @GetMapping("/mensuel/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadRapportMensuelPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        LocalDate d = date != null ? date : LocalDate.now();
        String filename = "Rapport_Mensuel_" + d.getYear() + "_"
                + String.format("%02d", d.getMonthValue()) + ".pdf";
        return createPdfResponse(
            rapportPdfService.genererRapportPeriodiquePDF(rapportService.getRapportMensuel(d)), filename);
    }

    @GetMapping("/annuel")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportAnnuel(
            @RequestParam(required = false) Integer annee) {
        int y = annee != null ? annee : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.success(rapportService.getRapportAnnuel(y)));
    }

    @GetMapping("/annuel/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadRapportAnnuelPDF(
            @RequestParam(required = false) Integer annee) {
        int y = annee != null ? annee : LocalDate.now().getYear();
        return createPdfResponse(
            rapportPdfService.genererRapportPeriodiquePDF(rapportService.getRapportAnnuel(y)),
            "Rapport_Annuel_" + y + ".pdf");
    }

    @GetMapping("/personnalise")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportPersonnalise(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return ResponseEntity.ok(ApiResponse.success(rapportService.getRapportPersonnalise(dateDebut, dateFin)));
    }

    @GetMapping("/personnalise/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<byte[]> downloadRapportPersonnalisePDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        return createPdfResponse(
            rapportPdfService.genererRapportPeriodiquePDF(rapportService.getRapportPersonnalise(dateDebut, dateFin)),
            "Rapport_" + dateDebut + "_" + dateFin + ".pdf");
    }

    // ==================== UTILITAIRES ====================

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdf, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        return new ResponseEntity<>(pdf, headers, HttpStatus.OK);
    }

    /**
     * Récupère l'UUID du vendeur connecté.
     * Le principal est directement l'entité User (via UserDetailsServiceImpl
     * qui retourne userRepository.findByEmail(...))
     */
    private String getVendeurId(Authentication authentication) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("Utilisateur non authentifié");
        }
        // Le principal EST l'entité User grâce à UserDetailsServiceImpl
        if (authentication.getPrincipal() instanceof User user) {
            return user.getId();
        }
        throw new IllegalStateException("Principal inattendu : " + authentication.getPrincipal().getClass());
    }
}