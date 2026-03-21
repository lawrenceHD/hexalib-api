package com.hexalib.api.comptabilite.dto;
 
import com.hexalib.api.comptabilite.model.CategorieDepense;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
 
import java.time.LocalDateTime;
 
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategorieDepenseResponse {
    private String id;
    private String nom;
    private String description;
    private CategorieDepense.Statut statut;
    private LocalDateTime createdAt;
 
    public static CategorieDepenseResponse fromEntity(CategorieDepense c) {
        return CategorieDepenseResponse.builder()
                .id(c.getId())
                .nom(c.getNom())
                .description(c.getDescription())
                .statut(c.getStatut())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
 