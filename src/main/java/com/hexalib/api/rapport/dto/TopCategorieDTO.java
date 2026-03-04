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
public class TopCategorieDTO {
    
    private String categorieId;
    private String nom;
    private String code;
    
    private long quantiteVendue;
    private BigDecimal chiffreAffaires;
    private long nombreVentes;
    
    private Integer rang;
}