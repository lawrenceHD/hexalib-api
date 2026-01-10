package com.hexalib.api.fournisseur.dto;

import com.hexalib.api.fournisseur.model.Fournisseur;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurResponse {
    private String id;
    private String nom;
    private String contact;
    private String telephone;
    private String email;
    private String adresse;
    private Integer delaiLivraisonJours;
    private Fournisseur.Statut statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static FournisseurResponse fromEntity(Fournisseur fournisseur) {
        return new FournisseurResponse(
                fournisseur.getId(),
                fournisseur.getNom(),
                fournisseur.getContact(),
                fournisseur.getTelephone(),
                fournisseur.getEmail(),
                fournisseur.getAdresse(),
                fournisseur.getDelaiLivraisonJours(),
                fournisseur.getStatut(),
                fournisseur.getCreatedAt(),
                fournisseur.getUpdatedAt()
        );
    }

    /**
     * Version simplifiée pour les listes (sans tous les détails)
     */
    public static FournisseurResponse fromEntitySimple(Fournisseur fournisseur) {
        FournisseurResponse response = new FournisseurResponse();
        response.setId(fournisseur.getId());
        response.setNom(fournisseur.getNom());
        response.setContact(fournisseur.getContact());
        response.setTelephone(fournisseur.getTelephone());
        response.setEmail(fournisseur.getEmail());
        response.setDelaiLivraisonJours(fournisseur.getDelaiLivraisonJours());
        response.setStatut(fournisseur.getStatut());
        response.setCreatedAt(fournisseur.getCreatedAt());
        return response;
    }
}