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
public class RapportStockValoriseDTO {
 
    private LocalDate dateGeneration;
 
    // Totaux globaux
    private BigDecimal valeurTotaleStockPrixVente;
    private int        totalLivresEnStock;
    private int        totalLivresEnRupture;
    private int        totalLivresCritiques;
 
    // Par catégorie
    private List<StockParCategorie> stockParCategorie;
 
    // Livres en rupture
    private List<LivreStockInfo> livresEnRupture;
 
    // Livres critiques (stock <= seuil)
    private List<LivreStockInfo> livresCritiques;
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class StockParCategorie {
        private String     categorieNom;
        private String     categorieCode;
        private int        nombreLivres;
        private int        quantiteTotale;
        private BigDecimal valeurPrixVente;
    }
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class LivreStockInfo {
        private String     code;
        private String     titre;
        private String     auteur;
        private String     categorieNom;
        private int        quantiteStock;
        private int        seuilMinimal;
        private BigDecimal prixVente;
    }
}
 