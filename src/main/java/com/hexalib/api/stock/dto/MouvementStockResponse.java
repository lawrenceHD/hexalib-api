package com.hexalib.api.stock.dto;

import com.hexalib.api.stock.model.TypeMouvement;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MouvementStockResponse {

    private String id;
    private String livreId;
    private String titreLivre;
    private String codeLivre;
    private TypeMouvement typeMouvement;
    private Integer quantite;
    private Integer stockAvant;
    private Integer stockApres;
    private String motif;
    private String reference;
    private UUID userId;
    private String userName;
    private LocalDateTime dateMouvement;
}