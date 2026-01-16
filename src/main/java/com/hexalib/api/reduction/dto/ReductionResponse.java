package com.hexalib.api.reduction.dto;

import com.hexalib.api.reduction.model.CibleReduction;
import com.hexalib.api.reduction.model.TypeReduction;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReductionResponse {

    private UUID id;
    private String intitule;
    private String description;
    private TypeReduction type;
    private BigDecimal valeur;
    private CibleReduction cible;
    private UUID cibleId;
    private String cibleNom; // Nom du livre ou catégorie si applicable
    private LocalDate dateDebut;
    private LocalDate dateFin;
    private Boolean actif;
    private Boolean estValide; // Calculé : actif + période
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}