
package com.hexalib.api.comptabilite.dto;
 
import com.hexalib.api.comptabilite.model.Depense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepenseResponse {
    private String id;
    private String description;
    private BigDecimal montant;
    private LocalDate dateDepense;
    private String categorieId;
    private String categorieNom;
    private String reference;
    private String enregistreParNom;
    private LocalDateTime createdAt;
 
    public static DepenseResponse fromEntity(Depense d) {
        return DepenseResponse.builder()
                .id(d.getId())
                .description(d.getDescription())
                .montant(d.getMontant())
                .dateDepense(d.getDateDepense())
                .categorieId(d.getCategorie().getId())
                .categorieNom(d.getCategorie().getNom())
                .reference(d.getReference())
                .enregistreParNom(d.getEnregistrePar() != null
                        ? d.getEnregistrePar().getNomComplet() : null)
                .createdAt(d.getCreatedAt())
                .build();
    }
}