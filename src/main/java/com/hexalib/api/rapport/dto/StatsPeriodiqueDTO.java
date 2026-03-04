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
public class StatsPeriodiqueDTO {
    private long nombreVentes;
    private BigDecimal chiffreAffaires;
    private BigDecimal montantReductions;
    private long nombreLivresVendus;
    private BigDecimal margeBeneficiaire;
    private BigDecimal panierMoyen;
}