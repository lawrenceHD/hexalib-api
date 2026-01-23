package com.hexalib.api.vente.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LigneVenteResponse {

    private String id;
    private String livreId;
    private String titreLivre;
    private String codeLivre;
    private BigDecimal prixUnitaire;
    private Integer quantite;
    private String reductionId;
    private String reductionIntitule;
    private BigDecimal montantReduction;
    private BigDecimal sousTotal;
}