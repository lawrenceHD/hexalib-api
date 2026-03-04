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
public class DashboardAdminDTO {
    
    // KPIs du jour
    private BigDecimal caJour;
    private long nombreVentesJour;
    
    // KPIs du mois
    private BigDecimal caMois;
    private long nombreVentesMois;
    
    // Stock critique
    private long nombreLivresStockCritique;
    private List<LivreStockCritiqueDTO> livresStockCritique;
    
    // Évolution CA (7 derniers jours)
    private List<EvolutionCADTO> evolutionCA7Jours;
    
    // Top 5 livres du mois
    private List<TopLivreDTO> top5LivresMois;
    
    // Performance vendeurs
    private List<PerformanceVendeurDTO> performanceVendeurs;
    
    // Statistiques globales
    private long totalLivresCatalogue;
    private long totalCategories;
    private long totalVendeurs;
}