package com.hexalib.api.livre.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ImportResultResponse {

    private int totalLignesLues;
    private int livresAjoutes;
    private int lignesIncompletes;
    private int doublonsTrouves;

    // Livres qui existaient déjà (titre + auteur + prix identiques)
    private List<DoublonInfo> doublons;

    // Détail des lignes ignorées (optionnel, pour debug)
    private List<String> lignesIgnoreesDetail;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class DoublonInfo {
        private String titre;
        private String auteur;
        private String prixVente;
        private int numeroLigne; // Numéro de ligne dans le fichier Excel
    }
}