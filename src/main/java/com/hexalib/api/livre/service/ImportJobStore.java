package com.hexalib.api.livre.service;

import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ImportJobStore {

    public record LigneParsee(
            int    numeroLigne,
            String categorie,
            String titre,
            String auteur,
            String maisonEdition,
            java.math.BigDecimal prixVente,
            boolean inactif,
            String description
    ) {}

    public record JobData(
            String              jobId,
            List<LigneParsee>   lignes,
            int                 batchSize,
            // Clé : nom catégorie en lowercase → Valeur : ID en base
            // On stocke UNIQUEMENT l'ID (pas l'entité) pour éviter
            // les problèmes d'entités Hibernate détachées entre les batches
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