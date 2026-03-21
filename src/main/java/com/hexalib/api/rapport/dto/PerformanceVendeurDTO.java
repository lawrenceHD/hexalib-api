package com.hexalib.api.rapport.dto;

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

    // Constructeur utilisé par JPQL
    // COUNT() → Long, SUM(BigDecimal) → BigDecimal, SUM(Integer) → Long
    public PerformanceVendeurDTO(String vendeurId,
                                 String nomComplet,
                                 Long nombreVentes,
                                 BigDecimal chiffreAffaires,
                                 Long nombreLivresVendus) {
        this.vendeurId          = vendeurId;
        this.nomComplet         = nomComplet;
        this.nombreVentes       = nombreVentes != null ? nombreVentes : 0L;
        this.chiffreAffaires    = chiffreAffaires != null ? chiffreAffaires : BigDecimal.ZERO;
        this.nombreLivresVendus = nombreLivresVendus != null ? nombreLivresVendus : 0L;
        // Panier moyen calculé côté service, pas en JPQL
        this.panierMoyen        = BigDecimal.ZERO;
        this.rang               = 0;
    }
}