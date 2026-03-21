package com.hexalib.api.comptabilite.dto;
 
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RapportVentesDTO {
 
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private String    typeRapport; // "AVEC_REDUCTION", "SANS_REDUCTION", "COMBINE"
 
    // Stats globales
    private long       nombreVentes;
    private BigDecimal caTotal;
    private BigDecimal totalReductions;
    private BigDecimal caNnet;
 
    // Stats ventes avec réduction (si COMBINE ou AVEC_REDUCTION)
    private long       nombreVentesAvecReduction;
    private BigDecimal caVentesAvecReduction;
    private BigDecimal montantReductionsAccordees;
 
    // Stats ventes sans réduction (si COMBINE ou SANS_REDUCTION)
    private long       nombreVentesSansReduction;
    private BigDecimal caVentesSansReduction;
 
    // Liste des ventes
    private List<LigneVenteRapport> ventes;
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LigneVenteRapport {
        private String        numeroFacture;
        private LocalDateTime dateVente;
        private String        vendeurNom;
        private BigDecimal    montantHT;
        private BigDecimal    montantReduction;
        private BigDecimal    montantTTC;
        private int           nombreArticles;
        private boolean       aReduction;
    }
}
 
