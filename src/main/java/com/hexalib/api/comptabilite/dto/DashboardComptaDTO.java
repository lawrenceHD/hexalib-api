package com.hexalib.api.comptabilite.dto;
 
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
public class DashboardComptaDTO {
 
    // Période couverte
    private String periode;
 
    // Entrées
    private BigDecimal totalEntrees;      // CA ventes validées
    private long      nombreVentes;
 
    // Sorties
    private BigDecimal totalSorties;      // Total dépenses
    private long      nombreDepenses;
 
    // Solde
    private BigDecimal soldeNet;          // Entrées − Sorties
 
    // Réductions accordées
    private BigDecimal totalReductions;
 
    // Répartition dépenses par catégorie
    private List<DepenseParCategorie> depensesParCategorie;
 
    // Top 5 catégories
    private List<DepenseParCategorie> top5Categories;
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DepenseParCategorie {
        private String     categorieNom;
        private BigDecimal montant;
        private long       nombreDepenses;
        private BigDecimal pourcentage;
    }
}
 