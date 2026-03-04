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
public class RapportJournalierDTO {
    
    private LocalDate date;
    private long nombreVentes;
    private BigDecimal chiffreAffaires;
    private BigDecimal montantReductions;
    private long nombreLivresVendus;
    
    // CA par vendeur
    private List<PerformanceVendeurDTO> caParVendeur;
    
    // Top livres et catégories
    private List<TopLivreDTO> topLivres;
    private List<TopCategorieDTO> topCategories;
    
    // Alertes stock
    private List<LivreStockCritiqueDTO> alertesStock;
}