package com.hexalib.api.livre.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

public class ImportJobDTO {

    // ── Réponse initiale après upload ──────────────────────────────
    @Data @Builder
    public static class ImportJobStarted {
        private String jobId;
        private int    totalLignes;
        private int    totalBatches;
        private int    batchSize;
    }

    // ── Réponse après chaque batch ─────────────────────────────────
    @Data @Builder
    public static class ImportBatchResult {
        private String  jobId;
        private int     batchNumero;
        private int     totalBatches;
        private int     traites;        // lignes traitées jusqu'ici
        private int     total;          // total de lignes
        private int     pourcentage;    // 0-100
        private boolean termine;
        // Présent uniquement quand termine = true
        private ImportRapportFinal rapport;
    }

    // ── Rapport final ──────────────────────────────────────────────
    @Data @Builder
    public static class ImportRapportFinal {
        private int              totalLignesLues;
        private int              categoriesCrees;
        private int              categoriesExistantes;
        private int              livresAjoutes;
        private int              livresIgnores;      // doublons
        private int              lignesIgnorees;     // sans titre
        private List<LigneErreur> erreurs;
    }

    @Data @Builder
    public static class LigneErreur {
        private int    numeroLigne;
        private String titre;
        private String raison;
    }
}