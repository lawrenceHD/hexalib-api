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
public class RapportTresorerieDTO {
 
    private LocalDate dateDebut;
    private LocalDate dateFin;
 
    // Totaux
    private BigDecimal totalEntrees;
    private BigDecimal totalSorties;
    private BigDecimal soldeNet;
 
    // Flux chronologiques (entrées + sorties mélangées, triées par date)
    private List<FluxTresorerie> flux;
 
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FluxTresorerie {
        private LocalDate  date;
        private String     type;        // "ENTREE" ou "SORTIE"
        private String     libelle;     // Description ou N° facture
        private String     categorie;  // Catégorie de dépense ou "Vente"
        private BigDecimal montant;
        private String     reference;  // N° facture ou référence dépense
        private String     enregistrePar;
    }
}
 