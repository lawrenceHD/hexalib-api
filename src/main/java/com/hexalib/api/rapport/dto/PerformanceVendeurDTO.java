package com.hexalib.api.rapport.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Data
@NoArgsConstructor


public class PerformanceVendeurDTO {

    private String vendeurId;
    private String nomComplet;
    private long nombreVentes;
    private BigDecimal chiffreAffaires;
    private long nombreLivresVendus;
    private BigDecimal panierMoyen;
    private Integer rang;

    // AVG() → Double, SUM(quantite Integer) → Long, COUNT() → Long, littéral 0 → Integer
    public PerformanceVendeurDTO(String vendeurId, String nomComplet,
                                 Long nombreVentes,
                                 BigDecimal chiffreAffaires,
                                 Long nombreLivresVendus,
                                 Double panierMoyen,
                                 Integer rang) {
        this.vendeurId          = vendeurId;
        this.nomComplet         = nomComplet;
        this.nombreVentes       = nombreVentes != null ? nombreVentes : 0L;
        this.chiffreAffaires    = chiffreAffaires != null ? chiffreAffaires : BigDecimal.ZERO;
        this.nombreLivresVendus = nombreLivresVendus != null ? nombreLivresVendus : 0L;
        this.panierMoyen        = panierMoyen != null
                ? BigDecimal.valueOf(panierMoyen).setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.rang = rang;
    }
}