package com.hexalib.api.vente.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VenteRequest {

    @NotEmpty(message = "La vente doit contenir au moins un livre")
    @Valid
    private List<LigneVenteRequest> lignes;
}