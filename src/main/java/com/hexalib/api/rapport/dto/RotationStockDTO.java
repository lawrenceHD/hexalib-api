package com.hexalib.api.rapport.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor

public class RotationStockDTO {

    private String categorieNom;
    private long quantiteVendue;
    private Integer stockActuel;
    private BigDecimal tauxRotation;

    // SUM(lv.quantite) → Long, SUM(l.quantiteStock) → Long, littéral 0 → Integer
    public RotationStockDTO(String categorieNom,
                            Long quantiteVendue,
                            Long stockActuel,
                            Integer tauxRotation) {
        this.categorieNom  = categorieNom;
        this.quantiteVendue = quantiteVendue != null ? quantiteVendue : 0L;
        this.stockActuel   = stockActuel != null ? stockActuel.intValue() : 0;
        this.tauxRotation  = BigDecimal.ZERO; // Calculé dans StatistiqueService
    }
}