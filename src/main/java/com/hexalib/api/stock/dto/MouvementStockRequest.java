package com.hexalib.api.stock.dto;

import com.hexalib.api.stock.model.TypeMouvement;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementStockRequest {

    @NotNull(message = "L'ID du livre est obligatoire")
    private UUID livreId;

    @NotNull(message = "Le type de mouvement est obligatoire")
    private TypeMouvement typeMouvement;

    @NotNull(message = "La quantité est obligatoire")
    @Min(value = 1, message = "La quantité doit être au moins 1")
    private Integer quantite;

    private String motif;

    private String reference;
}