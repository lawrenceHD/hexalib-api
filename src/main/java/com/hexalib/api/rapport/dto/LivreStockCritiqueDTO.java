package com.hexalib.api.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LivreStockCritiqueDTO {
    
    private String livreId;
    private String code;
    private String titre;
    private String auteur;
    private String categorie;
    
    private Integer quantiteStock;
    private Integer seuilMinimal;
    private String statutStock; // "CRITIQUE" ou "RUPTURE"
}