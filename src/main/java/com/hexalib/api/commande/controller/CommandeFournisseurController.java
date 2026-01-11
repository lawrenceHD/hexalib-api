package com.hexalib.api.commande.controller;

import com.hexalib.api.commande.dto.CommandeFournisseurRequest;
import com.hexalib.api.commande.dto.CommandeFournisseurResponse;
import com.hexalib.api.commande.service.CommandeFournisseurService;
import com.hexalib.api.commande.service.CommandePdfService;
import com.hexalib.api.common.dto.ApiResponse;
import com.hexalib.api.common.dto.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/commandes")
@RequiredArgsConstructor
@Tag(name = "Commandes Fournisseurs", description = "Gestion des commandes fournisseurs")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class CommandeFournisseurController {

    private final CommandeFournisseurService commandeService;
    private final CommandePdfService commandePdfService;

    @PostMapping
    @Operation(summary = "Créer une commande", description = "Créer une nouvelle commande fournisseur (Admin uniquement)")
    public ResponseEntity<ApiResponse<CommandeFournisseurResponse>> createCommande(
            @Valid @RequestBody CommandeFournisseurRequest request) {
        CommandeFournisseurResponse commande = commandeService.createCommande(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Commande créée avec succès", commande));
    }

    @GetMapping
    @Operation(summary = "Lister les commandes", description = "Récupérer toutes les commandes avec pagination et filtres")
    public ResponseEntity<ApiResponse<PageResponse<CommandeFournisseurResponse>>> getAllCommandes(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String fournisseurId,
            @RequestParam(required = false) String statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateDebut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateFin) {
        PageResponse<CommandeFournisseurResponse> commandes = commandeService.getAllCommandes(
            page, size, search, fournisseurId, statut, dateDebut, dateFin
        );
        return ResponseEntity.ok(ApiResponse.success(commandes));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une commande", description = "Récupérer les détails complets d'une commande par son ID")
    public ResponseEntity<ApiResponse<CommandeFournisseurResponse>> getCommandeById(@PathVariable String id) {
        CommandeFournisseurResponse commande = commandeService.getCommandeById(id);
        return ResponseEntity.ok(ApiResponse.success(commande));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une commande", description = "Modifier une commande en attente")
    public ResponseEntity<ApiResponse<CommandeFournisseurResponse>> updateCommande(
            @PathVariable String id,
            @Valid @RequestBody CommandeFournisseurRequest request) {
        CommandeFournisseurResponse commande = commandeService.updateCommande(id, request);
        return ResponseEntity.ok(ApiResponse.success("Commande modifiée avec succès", commande));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une commande", description = "Supprimer une commande en attente")
    public ResponseEntity<ApiResponse<Void>> deleteCommande(@PathVariable String id) {
        commandeService.deleteCommande(id);
        return ResponseEntity.ok(ApiResponse.success("Commande supprimée avec succès", null));
    }

    @PostMapping("/{id}/recevoir")
    @Operation(summary = "Recevoir une commande", description = "Marquer une commande comme reçue et mettre à jour les stocks")
    public ResponseEntity<ApiResponse<CommandeFournisseurResponse>> recevoirCommande(
            @PathVariable String id,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate dateReception) {
        CommandeFournisseurResponse commande = commandeService.recevoirCommande(id, dateReception);
        return ResponseEntity.ok(ApiResponse.success("Commande reçue avec succès. Les stocks ont été mis à jour.", commande));
    }

    @PostMapping("/{id}/annuler")
    @Operation(summary = "Annuler une commande", description = "Annuler une commande en attente")
    public ResponseEntity<ApiResponse<CommandeFournisseurResponse>> annulerCommande(
            @PathVariable String id,
            @RequestParam(required = false) String motif) {
        CommandeFournisseurResponse commande = commandeService.annulerCommande(id, motif);
        return ResponseEntity.ok(ApiResponse.success("Commande annulée avec succès", commande));
    }

    @GetMapping("/{id}/pdf")
    @Operation(summary = "Télécharger le PDF", description = "Générer et télécharger le bon de commande en PDF")
    public ResponseEntity<byte[]> downloadCommandePdf(@PathVariable String id) {
        byte[] pdfBytes = commandePdfService.generateCommandePdf(id);
        
        CommandeFournisseurResponse commande = commandeService.getCommandeById(id);
        String filename = "Commande_" + commande.getNumeroCommande() + ".pdf";

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("attachment", filename);
        headers.setCacheControl("must-revalidate, post-check=0, pre-check=0");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }

    @GetMapping("/{id}/pdf/preview")
    @Operation(summary = "Prévisualiser le PDF", description = "Afficher le bon de commande en PDF dans le navigateur")
    public ResponseEntity<byte[]> previewCommandePdf(@PathVariable String id) {
        byte[] pdfBytes = commandePdfService.generateCommandePdf(id);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        headers.setContentDispositionFormData("inline", "preview.pdf");

        return new ResponseEntity<>(pdfBytes, headers, HttpStatus.OK);
    }
}