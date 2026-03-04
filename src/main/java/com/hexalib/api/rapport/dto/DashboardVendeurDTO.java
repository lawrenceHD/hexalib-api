package com.hexalib.api.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardVendeurDTO {
    
    // Mes stats du jour
    private long mesVentesJour;
    private BigDecimal monCAJour;
    
    // Mes stats du mois
    private long mesVentesMois;
    private BigDecimal monCAMois;
    
    // Mes 5 meilleures ventes (du mois)
    private List<TopLivreDTO> mesMeilleuresVentes;
    
    // Alertes stock critique (pour info)
    private long nombreLivresStockCritique;
    
    // Mon classement (optionnel)
    private Integer monClassement; // Position parmi les vendeurs
    private BigDecimal objectifMensuel; // Si défini
    private BigDecimal tauxAtteinte; // % objectif atteint
}