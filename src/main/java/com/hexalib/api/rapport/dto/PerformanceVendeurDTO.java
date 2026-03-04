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
public class PerformanceVendeurDTO {
    
    private String vendeurId;
    private String nomComplet;
    
    private long nombreVentes;
    private BigDecimal chiffreAffaires;
    private long nombreLivresVendus;
    
    private BigDecimal panierMoyen; // CA / Nombre de ventes
    private Integer rang; // Classement
}