package com.hexalib.api.commande.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommandeRequest {

    @NotBlank(message = "Le livre est obligatoire")
    private String livreId;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    @NotNull(message = "Le prix d'achat unitaire est obligatoire")
    @DecimalMin(value = "0.01", message = "Le prix d'achat doit être supérieur à 0")
    private BigDecimal prixAchatUnitaire;
}