package com.hexalib.api.rapport.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Builder
public class TopCategorieDTO {

    private String categorieId;
    private String nom;
    private String code;
    private long quantiteVendue;
    private BigDecimal chiffreAffaires;
    private long nombreVentes;
    private Integer rang;

    // SUM(lv.quantite) → Long, SUM(lv.sousTotal) → BigDecimal,
    // COUNT(DISTINCT v.id) → Long, littéral 0 → Integer
    public TopCategorieDTO(String categorieId, String nom, String code,
                           Long quantiteVendue,
                           BigDecimal chiffreAffaires,
                           Long nombreVentes,
                           Integer rang) {
        this.categorieId     = categorieId;
        this.nom             = nom;
        this.code            = code;
        this.quantiteVendue  = quantiteVendue != null ? quantiteVendue : 0L;
        this.chiffreAffaires = chiffreAffaires != null ? chiffreAffaires : BigDecimal.ZERO;
        this.nombreVentes    = nombreVentes != null ? nombreVentes : 0L;
        this.rang            = rang;
    }
}