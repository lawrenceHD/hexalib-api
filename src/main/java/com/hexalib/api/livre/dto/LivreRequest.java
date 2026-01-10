package com.hexalib.api.livre.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LivreRequest {

    @NotBlank(message = "Le titre du livre est obligatoire")
    @Size(min = 2, max = 255, message = "Le titre doit contenir entre 2 et 255 caractères")
    private String titre;

    @Size(max = 2000, message = "La description ne peut pas dépasser 2000 caractères")
    private String description;

    @NotBlank(message = "Le nom de l'auteur est obligatoire")
    @Size(min = 2, max = 100, message = "Le nom de l'auteur doit contenir entre 2 et 100 caractères")
    private String auteur;

    @NotBlank(message = "La maison d'édition est obligatoire")
    @Size(min = 2, max = 100, message = "La maison d'édition doit contenir entre 2 et 100 caractères")
    private String maisonEdition;

    @Past(message = "La date de parution doit être dans le passé")
    private LocalDate dateParution;

    @Pattern(
        regexp = "^(?:ISBN(?:-1[03])?:? )?(?=[0-9X]{10}$|(?=(?:[0-9]+[- ]){3})[- 0-9X]{13}$|97[89][0-9]{10}$|(?=(?:[0-9]+[- ]){4})[- 0-9]{17}$)(?:97[89][- ]?)?[0-9]{1,5}[- ]?[0-9]+[- ]?[0-9]+[- ]?[0-9X]$",
        message = "Le format ISBN est invalide"
    )
    private String isbn;

    @NotBlank(message = "La langue est obligatoire")
    @Size(min = 2, max = 50, message = "La langue doit contenir entre 2 et 50 caractères")
    private String langue;

    @NotNull(message = "La quantité en stock est obligatoire")
    @Min(value = 0, message = "La quantité en stock ne peut pas être négative")
    private Integer quantiteStock;

    @NotNull(message = "Le seuil minimal est obligatoire")
    @Min(value = 0, message = "Le seuil minimal ne peut pas être négatif")
    private Integer seuilMinimal;

    @NotNull(message = "Le prix de vente est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix de vente doit être supérieur à 0")
    private BigDecimal prixVente;

    @DecimalMin(value = "0.01", message = "Le prix d'achat doit être supérieur à 0")
    private BigDecimal prixAchat;

    @Size(max = 50, message = "L'emplacement ne peut pas dépasser 50 caractères")
    private String emplacement;

    @NotBlank(message = "La catégorie est obligatoire")
    private String categorieId;
}