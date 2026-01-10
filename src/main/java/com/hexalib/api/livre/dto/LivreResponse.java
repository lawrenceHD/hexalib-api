package com.hexalib.api.livre.dto;

import com.hexalib.api.categorie.dto.CategorieResponse;
import com.hexalib.api.livre.model.Livre;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivreResponse {
    private String id;
    private String code;
    private String titre;
    private String description;
    private String auteur;
    private String maisonEdition;
    private LocalDate dateParution;
    private String isbn;
    private String langue;
    private Integer quantiteStock;
    private Integer seuilMinimal;
    private BigDecimal prixVente;
    private BigDecimal prixAchat;
    private String emplacement;
    private CategorieResponse categorie;
    private Livre.Statut statut;
    private Livre.StatutStock statutStock;
    private boolean stockCritique;
    private boolean enRupture;
    private BigDecimal marge;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static LivreResponse fromEntity(Livre livre) {
        CategorieResponse categorieResponse = CategorieResponse.fromEntity(livre.getCategorie());
        
        return new LivreResponse(
                livre.getId(),
                livre.getCode(),
                livre.getTitre(),
                livre.getDescription(),
                livre.getAuteur(),
                livre.getMaisonEdition(),
                livre.getDateParution(),
                livre.getIsbn(),
                livre.getLangue(),
                livre.getQuantiteStock(),
                livre.getSeuilMinimal(),
                livre.getPrixVente(),
                livre.getPrixAchat(),
                livre.getEmplacement(),
                categorieResponse,
                livre.getStatut(),
                livre.getStatutStock(),
                livre.isStockCritique(),
                livre.isEnRupture(),
                livre.getMarge(),
                livre.getCreatedAt(),
                livre.getUpdatedAt()
        );
    }

    /**
     * Version simplifiée pour les listes (sans relations complètes)
     */
    public static LivreResponse fromEntitySimple(Livre livre) {
        LivreResponse response = new LivreResponse();
        response.setId(livre.getId());
        response.setCode(livre.getCode());
        response.setTitre(livre.getTitre());
        response.setAuteur(livre.getAuteur());
        response.setMaisonEdition(livre.getMaisonEdition());
        response.setDateParution(livre.getDateParution());
        response.setLangue(livre.getLangue());
        response.setQuantiteStock(livre.getQuantiteStock());
        response.setSeuilMinimal(livre.getSeuilMinimal());
        response.setPrixVente(livre.getPrixVente());
        response.setStatut(livre.getStatut());
        response.setStatutStock(livre.getStatutStock());
        response.setStockCritique(livre.isStockCritique());
        response.setEnRupture(livre.isEnRupture());
        response.setCreatedAt(livre.getCreatedAt());
        
        // Catégorie simplifiée (juste id et nom)
        CategorieResponse catSimple = new CategorieResponse();
        catSimple.setId(livre.getCategorie().getId());
        catSimple.setNom(livre.getCategorie().getNom());
        catSimple.setCode(livre.getCategorie().getCode());
        response.setCategorie(catSimple);
        
        return response;
    }
}