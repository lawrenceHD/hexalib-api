package com.hexalib.api.livre.service;

import com.hexalib.api.livre.dto.ImportJobDTO;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Stocke en mémoire les données pré-parsées d'un import Excel.
 * Chaque job a une durée de vie courte (nettoyé après complétion).
 */
@Component
public class ImportJobStore {

    // Ligne brute parsée depuis Excel, prête à être insérée
    public record LigneParsee(
            int    numeroLigne,
            String categorie,
            String titre,
            String auteur,
            String maisonEdition,
            java.math.BigDecimal prixVente,   // null si xx ou 0
            boolean inactif,                  // true si prix = 0
            String description
    ) {}

    public record JobData(
            String              jobId,
            List<LigneParsee>   lignes,
            int                 batchSize
    ) {}

    private final Map<String, JobData> jobs = new ConcurrentHashMap<>();

    public String creerJob(List<LigneParsee> lignes, int batchSize) {
        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new JobData(jobId, lignes, batchSize));
        return jobId;
    }

    public JobData getJob(String jobId) {
        return jobs.get(jobId);
    }

    public void supprimerJob(String jobId) {
        jobs.remove(jobId);
    }
}