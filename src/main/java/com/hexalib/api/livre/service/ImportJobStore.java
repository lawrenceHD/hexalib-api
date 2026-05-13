package com.hexalib.api.livre.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImportJobStore {

    public record LigneParsee(
            int              numeroLigne,
            String           categorie,
            String           titre,
            String           auteur,
            String           maisonEdition,
            java.math.BigDecimal prixVente,
            java.math.BigDecimal prixAchat,   // ← colonne F (prix net)
            boolean          inactif,         // conservé pour compatibilité (plus utilisé)
            String           description
    ) {}

    public record JobData(
            String              jobId,
            List<LigneParsee>   lignes,
            int                 batchSize,
            Map<String, String> cacheCategories
    ) {}

    private final Map<String, JobData> jobs = new ConcurrentHashMap<>();

    public String creerJob(List<LigneParsee> lignes, int batchSize) {
        String jobId = UUID.randomUUID().toString();
        jobs.put(jobId, new JobData(
                jobId,
                lignes,
                batchSize,
                new ConcurrentHashMap<>()
        ));
        return jobId;
    }

    public JobData getJob(String jobId) {
        return jobs.get(jobId);
    }

    public void supprimerJob(String jobId) {
        jobs.remove(jobId);
    }
}