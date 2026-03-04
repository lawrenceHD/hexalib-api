package com.hexalib.api.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TopLivreDTO {
    
    private String livreId;
    private String code;
    private String titre;
    private String auteur;
    private String categorie;
    
    private long quantiteVendue;
    private BigDecimal chiffreAffaires;
    private long nombreVentes;
    
    private Integer rang; // Position dans le classement
}