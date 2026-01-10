package com.hexalib.api.fournisseur.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FournisseurRequest {

    @NotBlank(message = "Le nom du fournisseur est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom doit contenir entre 2 et 100 caractères")
    private String nom;

    @Size(max = 100, message = "Le nom du contact ne peut pas dépasser 100 caractères")
    private String contact;

    @Pattern(
        regexp = "^[+]?[0-9]{8,20}$",
        message = "Le numéro de téléphone est invalide"
    )
    private String telephone;

    @Email(message = "L'adresse email est invalide")
    @Size(max = 100, message = "L'email ne peut pas dépasser 100 caractères")
    private String email;

    @Size(max = 500, message = "L'adresse ne peut pas dépasser 500 caractères")
    private String adresse;

    @Min(value = 0, message = "Le délai de livraison ne peut pas être négatif")
    @Max(value = 365, message = "Le délai de livraison ne peut pas dépasser 365 jours")
    private Integer delaiLivraisonJours;
}