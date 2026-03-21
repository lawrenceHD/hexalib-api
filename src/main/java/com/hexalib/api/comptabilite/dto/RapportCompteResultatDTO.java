package com.hexalib.api.comptabilite.dto;
 
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
public class RapportCompteResultatDTO {
 
    private LocalDate dateDebut;
    private LocalDate dateFin;
 
    // Produits (entrées)
    private BigDecimal caVentes;
    private BigDecimal totalReductionsAccordees;
    private BigDecimal caNet;
 
    // Charges (sorties = dépenses)
    private BigDecimal totalCharges;
    private List<ChargeParCategorie> chargesParCategorie;
 
    // Résultat
    private BigDecimal resultatNet;    // caNet − totalCharges
    private boolean    beneficiaire;  // resultatNet > 0
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ChargeParCategorie {
        private String     categorieNom;
        private BigDecimal montant;
        private long       nombreDepenses;
        private BigDecimal pourcentageDesCharges;
    }
}
 