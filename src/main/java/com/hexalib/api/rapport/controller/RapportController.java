package com.hexalib.api.rapport.controller;

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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/rapports")
@RequiredArgsConstructor
@Tag(name = "Rapports", description = "Gestion des rapports et statistiques")
@SecurityRequirement(name = "bearerAuth")
public class RapportController {

    private final RapportService rapportService;
    private final RapportPdfService rapportPdfService;

    // ==================== DASHBOARDS ====================

    /**
     * Dashboard Admin
     */
    @GetMapping("/dashboard/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Dashboard Admin", description = "Récupérer le tableau de bord administrateur avec KPIs et graphiques")
    public ResponseEntity<ApiResponse<DashboardAdminDTO>> getDashboardAdmin() {
        DashboardAdminDTO dashboard = rapportService.getDashboardAdmin();
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    /**
     * Dashboard Vendeur
     */
    @GetMapping("/dashboard/vendeur")
    @Operation(summary = "Dashboard Vendeur", description = "Récupérer le tableau de bord du vendeur connecté")
    public ResponseEntity<ApiResponse<DashboardVendeurDTO>> getDashboardVendeur() {
        // Récupérer l'ID du vendeur connecté
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String vendeurId = getUserIdFromAuthentication(authentication);
        
        DashboardVendeurDTO dashboard = rapportService.getDashboardVendeur(vendeurId);
        return ResponseEntity.ok(ApiResponse.success(dashboard));
    }

    // ==================== RAPPORT JOURNALIER ====================

    /**
     * Rapport de clôture journalière (JSON)
     */
    @GetMapping("/cloture-journaliere")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport journalier JSON", description = "Récupérer le rapport de clôture journalière au format JSON")
    public ResponseEntity<ApiResponse<RapportJournalierDTO>> getRapportJournalier(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        RapportJournalierDTO rapport = rapportService.getRapportJournalier(targetDate);
        
        return ResponseEntity.ok(ApiResponse.success(rapport));
    }

    /**
     * Rapport de clôture journalière (PDF)
     */
    @GetMapping("/cloture-journaliere/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport journalier PDF", description = "Télécharger le rapport de clôture journalière au format PDF")
    public ResponseEntity<byte[]> downloadRapportJournalierPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        RapportJournalierDTO rapport = rapportService.getRapportJournalier(targetDate);
        byte[] pdfBytes = rapportPdfService.genererRapportJournalierPDF(rapport);
        
        String filename = "Rapport_Journalier_" + targetDate.toString() + ".pdf";
        
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    // ==================== RAPPORTS PÉRIODIQUES ====================

    /**
     * Rapport hebdomadaire (JSON)
     */
    @GetMapping("/hebdomadaire")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport hebdomadaire JSON", description = "Récupérer le rapport hebdomadaire (7 derniers jours)")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportHebdomadaire(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        LocalDate targetDate = dateFin != null ? dateFin : LocalDate.now();
        RapportPeriodiqueDTO rapport = rapportService.getRapportHebdomadaire(targetDate);
        
        return ResponseEntity.ok(ApiResponse.success(rapport));
    }

    /**
     * Rapport hebdomadaire (PDF)
     */
    @GetMapping("/hebdomadaire/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport hebdomadaire PDF", description = "Télécharger le rapport hebdomadaire au format PDF")
    public ResponseEntity<byte[]> downloadRapportHebdomadairePDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        LocalDate targetDate = dateFin != null ? dateFin : LocalDate.now();
        RapportPeriodiqueDTO rapport = rapportService.getRapportHebdomadaire(targetDate);
        byte[] pdfBytes = rapportPdfService.genererRapportPeriodiquePDF(rapport);
        
        String filename = "Rapport_Hebdomadaire_" + targetDate.toString() + ".pdf";
        
        return createPdfResponse(pdfBytes, filename);
    }

    /**
     * Rapport mensuel (JSON)
     */
    @GetMapping("/mensuel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport mensuel JSON", description = "Récupérer le rapport mensuel")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportMensuel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        RapportPeriodiqueDTO rapport = rapportService.getRapportMensuel(targetDate);
        
        return ResponseEntity.ok(ApiResponse.success(rapport));
    }

    /**
     * Rapport mensuel (PDF)
     */
    @GetMapping("/mensuel/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport mensuel PDF", description = "Télécharger le rapport mensuel au format PDF")
    public ResponseEntity<byte[]> downloadRapportMensuelPDF(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        
        LocalDate targetDate = date != null ? date : LocalDate.now();
        RapportPeriodiqueDTO rapport = rapportService.getRapportMensuel(targetDate);
        byte[] pdfBytes = rapportPdfService.genererRapportPeriodiquePDF(rapport);
        
        String filename = "Rapport_Mensuel_" + targetDate.getYear() + "_" + 
                         String.format("%02d", targetDate.getMonthValue()) + ".pdf";
        
        return createPdfResponse(pdfBytes, filename);
    }

    /**
     * Rapport annuel (JSON)
     */
    @GetMapping("/annuel")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport annuel JSON", description = "Récupérer le rapport annuel")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportAnnuel(
            @RequestParam(required = false) Integer annee) {
        
        int targetYear = annee != null ? annee : LocalDate.now().getYear();
        RapportPeriodiqueDTO rapport = rapportService.getRapportAnnuel(targetYear);
        
        return ResponseEntity.ok(ApiResponse.success(rapport));
    }

    /**
     * Rapport annuel (PDF)
     */
    @GetMapping("/annuel/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport annuel PDF", description = "Télécharger le rapport annuel au format PDF")
    public ResponseEntity<byte[]> downloadRapportAnnuelPDF(
            @RequestParam(required = false) Integer annee) {
        
        int targetYear = annee != null ? annee : LocalDate.now().getYear();
        RapportPeriodiqueDTO rapport = rapportService.getRapportAnnuel(targetYear);
        byte[] pdfBytes = rapportPdfService.genererRapportPeriodiquePDF(rapport);
        
        String filename = "Rapport_Annuel_" + targetYear + ".pdf";
        
        return createPdfResponse(pdfBytes, filename);
    }

    /**
     * Rapport personnalisé (JSON)
     */
    @GetMapping("/personnalise")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport personnalisé JSON", description = "Récupérer un rapport pour une période personnalisée")
    public ResponseEntity<ApiResponse<RapportPeriodiqueDTO>> getRapportPersonnalise(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        RapportPeriodiqueDTO rapport = rapportService.getRapportPersonnalise(dateDebut, dateFin);
        return ResponseEntity.ok(ApiResponse.success(rapport));
    }

    /**
     * Rapport personnalisé (PDF)
     */
    @GetMapping("/personnalise/pdf")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Rapport personnalisé PDF", description = "Télécharger un rapport personnalisé au format PDF")
    public ResponseEntity<byte[]> downloadRapportPersonnalisePDF(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        
        RapportPeriodiqueDTO rapport = rapportService.getRapportPersonnalise(dateDebut, dateFin);
        byte[] pdfBytes = rapportPdfService.genererRapportPeriodiquePDF(rapport);
        
        String filename = "Rapport_" + dateDebut + "_" + dateFin + ".pdf";
        
        return createPdfResponse(pdfBytes, filename);
    }

    // ==================== MÉTHODES UTILITAIRES ====================

    private ResponseEntity<byte[]> createPdfResponse(byte[] pdfBytes, String filename) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");
        
        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    private String getUserIdFromAuthentication(Authentication authentication) {
        // TODO: Adapter selon votre implémentation de récupération de l'ID utilisateur
        // Pour l'instant, retourner un ID fictif
        return "user-id-from-auth";
    }
}
