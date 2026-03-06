package com.hexalib.api.rapport.dto;

import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@Builder
public class TopLivreDTO {

    private String livreId;
    private String code;
    private String titre;
    private String auteur;
    private String categorie;
    private long quantiteVendue;
    private BigDecimal chiffreAffaires;
    private long nombreVentes;
    private Integer rang;

    // SUM(lv.quantite) → Long, SUM(lv.sousTotal) → BigDecimal,
    // COUNT(DISTINCT v.id) → Long, littéral 0 → Integer
    public TopLivreDTO(String livreId, String code, String titre, String auteur, String categorie,
                       Long quantiteVendue,
                       BigDecimal chiffreAffaires,
                       Long nombreVentes,
                       Integer rang) {
        this.livreId        = livreId;
        this.code           = code;
        this.titre          = titre;
        this.auteur         = auteur;
        this.categorie      = categorie;
        this.quantiteVendue = quantiteVendue != null ? quantiteVendue : 0L;
        this.chiffreAffaires = chiffreAffaires != null ? chiffreAffaires : BigDecimal.ZERO;
        this.nombreVentes   = nombreVentes != null ? nombreVentes : 0L;
        this.rang           = rang;
    }
}