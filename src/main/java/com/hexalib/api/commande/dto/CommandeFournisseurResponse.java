package com.hexalib.api.commande.dto;

import com.hexalib.api.commande.model.CommandeFournisseur;
import com.hexalib.api.commande.model.LigneCommandeFournisseur;
import com.hexalib.api.fournisseur.dto.FournisseurResponse;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeFournisseurResponse {
    private String id;
    private String numeroCommande;
    private FournisseurResponse fournisseur;
    private LocalDate dateCommande;
    private LocalDate dateReceptionPrevue;
    private LocalDate dateReceptionReelle;
    private BigDecimal montantTotal;
    private CommandeFournisseur.Statut statut;
    private String notes;
    private String createdByName;
    private List<LigneCommandeResponse> lignes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CommandeFournisseurResponse fromEntity(CommandeFournisseur commande) {
        CommandeFournisseurResponse response = new CommandeFournisseurResponse();
        response.setId(commande.getId());
        response.setNumeroCommande(commande.getNumeroCommande());
        response.setFournisseur(FournisseurResponse.fromEntity(commande.getFournisseur()));
        response.setDateCommande(commande.getDateCommande());
        response.setDateReceptionPrevue(commande.getDateReceptionPrevue());
        response.setDateReceptionReelle(commande.getDateReceptionReelle());
        response.setMontantTotal(commande.getMontantTotal());
        response.setStatut(commande.getStatut());
        response.setNotes(commande.getNotes());
        response.setCreatedByName(commande.getCreatedBy() != null ? commande.getCreatedBy().getNomComplet() : null);
        response.setLignes(
            commande.getLignes().stream()
                .map(LigneCommandeResponse::fromEntity)
                .collect(Collectors.toList())
        );
        response.setCreatedAt(commande.getCreatedAt());
        response.setUpdatedAt(commande.getUpdatedAt());
        return response;
    }

    /**
     * Version simplifiée pour les listes
     */
    public static CommandeFournisseurResponse fromEntitySimple(CommandeFournisseur commande) {
        CommandeFournisseurResponse response = new CommandeFournisseurResponse();
        response.setId(commande.getId());
        response.setNumeroCommande(commande.getNumeroCommande());
        
        FournisseurResponse fournisseurSimple = new FournisseurResponse();
        fournisseurSimple.setId(commande.getFournisseur().getId());
        fournisseurSimple.setNom(commande.getFournisseur().getNom());
        response.setFournisseur(fournisseurSimple);
        
        response.setDateCommande(commande.getDateCommande());
        response.setDateReceptionPrevue(commande.getDateReceptionPrevue());
        response.setDateReceptionReelle(commande.getDateReceptionReelle());
        response.setMontantTotal(commande.getMontantTotal());
        response.setStatut(commande.getStatut());
        response.setCreatedAt(commande.getCreatedAt());
        return response;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LigneCommandeResponse {
        private String id;
        private String livreId;
        private String livreTitre;
        private String livreCode;
        private Integer quantite;
        private BigDecimal prixAchatUnitaire;
        private BigDecimal sousTotal;

        public static LigneCommandeResponse fromEntity(LigneCommandeFournisseur ligne) {
            return new LigneCommandeResponse(
                ligne.getId(),
                ligne.getLivre().getId(),
                ligne.getLivre().getTitre(),
                ligne.getLivre().getCode(),
                ligne.getQuantite(),
                ligne.getPrixAchatUnitaire(),
                ligne.getSousTotal()
            );
        }
    }
}