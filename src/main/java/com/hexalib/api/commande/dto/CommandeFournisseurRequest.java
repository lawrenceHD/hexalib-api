package com.hexalib.api.commande.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeFournisseurRequest {

    @NotBlank(message = "Le fournisseur est obligatoire")
    private String fournisseurId;

    @NotNull(message = "La date de commande est obligatoire")
    private LocalDate dateCommande;

    private LocalDate dateReceptionPrevue;

    @Size(max = 1000, message = "Les notes ne peuvent pas dépasser 1000 caractères")
    private String notes;

    @NotEmpty(message = "La commande doit contenir au moins un livre")
    private List<LigneCommandeRequest> lignes;
}