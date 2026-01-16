package com.hexalib.api.reduction.dto;

import com.hexalib.api.reduction.model.CibleReduction;
import com.hexalib.api.reduction.model.TypeReduction;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReductionRequest {

    @NotBlank(message = "L'intitulé est obligatoire")
    @Size(max = 100, message = "L'intitulé ne peut pas dépasser 100 caractères")
    private String intitule;

    private String description;

    @NotNull(message = "Le type de réduction est obligatoire")
    private TypeReduction type;

    @NotNull(message = "La valeur est obligatoire")
    @DecimalMin(value = "0.0", inclusive = false, message = "La valeur doit être supérieure à 0")
    @DecimalMax(value = "100.0", message = "Le pourcentage ne peut pas dépasser 100")
    private BigDecimal valeur;

    @NotNull(message = "La cible est obligatoire")
    private CibleReduction cible;

    // Obligatoire si cible = LIVRE ou CATEGORIE
    private UUID cibleId;

    @NotNull(message = "La date de début est obligatoire")
    private LocalDate dateDebut;

    @NotNull(message = "La date de fin est obligatoire")
    private LocalDate dateFin;

    private Boolean actif = true;

    // Validation personnalisée
    @AssertTrue(message = "La date de fin doit être après la date de début")
    public boolean isDateFinValide() {
        if (dateDebut == null || dateFin == null) {
            return true; // Laisse les autres validations gérer les null
        }
        return !dateFin.isBefore(dateDebut);
    }

    @AssertTrue(message = "La cible ID est obligatoire pour les réductions spécifiques")
    public boolean isCibleIdValide() {
        if (cible == CibleReduction.GLOBALE) {
            return true;
        }
        return cibleId != null;
    }
}