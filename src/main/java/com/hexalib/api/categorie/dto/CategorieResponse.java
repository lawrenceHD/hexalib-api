package com.hexalib.api.categorie.dto;

import com.hexalib.api.categorie.model.Categorie;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategorieResponse {
    private String id;
    private String nom;
    private String description;
    private String code;
    private Categorie.Statut statut;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public static CategorieResponse fromEntity(Categorie categorie) {
        return new CategorieResponse(
                categorie.getId(),
                categorie.getNom(),
                categorie.getDescription(),
                categorie.getCode(),
                categorie.getStatut(),
                categorie.getCreatedAt(),
                categorie.getUpdatedAt()
        );
    }
}