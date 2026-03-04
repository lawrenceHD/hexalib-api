package com.hexalib.api.rapport.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportPeriodiqueDTO {
    
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String periode; // "HEBDOMADAIRE", "MENSUEL", "ANNUEL", "PERSONNALISE"
    
    // KPIs globaux
    private long nombreVentes;
    private BigDecimal chiffreAffaires;
    private BigDecimal montantReductions;
    private long nombreLivresVendus;
    private BigDecimal margeBeneficiaire;
    
    // Comparaison avec période précédente
    private BigDecimal evolutionCA; // En pourcentage
    private long evolutionNombreVentes;
    
    // Évolution CA (pour graphique)
    private List<EvolutionCADTO> evolutionCA7Jours;
    
    // Top performers
    private List<TopLivreDTO> topLivres;
    private List<TopCategorieDTO> topCategories;
    private List<PerformanceVendeurDTO> performanceVendeurs;
    
    // Analyse réductions
    private AnalyseReductionsDTO analyseReductions;
    
    // Rotation stock
    private List<RotationStockDTO> rotationStock;
}