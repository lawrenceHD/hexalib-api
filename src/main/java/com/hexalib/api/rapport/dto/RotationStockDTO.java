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
public class RotationStockDTO {
    
    private String categorieNom;
    private long quantiteVendue;
    private Integer stockActuel;
    private BigDecimal tauxRotation; // (Qté vendue / Stock actuel) * 100
}