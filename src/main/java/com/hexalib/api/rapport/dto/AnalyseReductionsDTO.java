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
public class AnalyseReductionsDTO {
    
    private BigDecimal montantTotalReductions;
    private long nombreVentesAvecReduction;
    private BigDecimal pourcentageVentesAvecReduction;
    
    private BigDecimal reductionMoyenne;
    private BigDecimal reductionMaximale;
}