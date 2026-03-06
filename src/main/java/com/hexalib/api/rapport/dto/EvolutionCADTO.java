package com.hexalib.api.rapport.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor

public class EvolutionCADTO {

    private LocalDate date;
    private BigDecimal chiffreAffaires;
    private long nombreVentes;

    // CAST(v.dateVente AS LocalDate) → LocalDate
    // SUM(v.montantTTC)              → BigDecimal
    // COUNT(v)                       → Long
    public EvolutionCADTO(LocalDate date,
                          BigDecimal chiffreAffaires,
                          Long nombreVentes) {
        this.date            = date;
        this.chiffreAffaires = chiffreAffaires != null ? chiffreAffaires : BigDecimal.ZERO;
        this.nombreVentes    = nombreVentes != null ? nombreVentes : 0L;
    }
}