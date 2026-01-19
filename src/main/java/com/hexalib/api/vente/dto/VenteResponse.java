package com.hexalib.api.vente.dto;

import com.hexalib.api.vente.model.StatutVente;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VenteResponse {

    private UUID id;
    private String numeroFacture;
    private LocalDateTime dateVente;
    private UUID vendeurId;
    private String vendeurNom;
    private BigDecimal montantHT;
    private BigDecimal montantReductions;
    private BigDecimal montantTTC;
    private StatutVente statut;
    private String motifAnnulation;
    private List<LigneVenteResponse> lignes;
    private LocalDateTime createdAt;
}