package com.hexalib.api.reduction.service;

import com.hexalib.api.categorie.repository.CategorieRepository;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import com.hexalib.api.reduction.dto.ReductionRequest;
import com.hexalib.api.reduction.dto.ReductionResponse;
import com.hexalib.api.reduction.model.CibleReduction;
import com.hexalib.api.reduction.model.Reduction;
import com.hexalib.api.reduction.repository.ReductionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ReductionService {

    private final ReductionRepository reductionRepository;
    private final LivreRepository livreRepository;
    private final CategorieRepository categorieRepository;

    public ReductionResponse create(ReductionRequest request) {
        log.info("Création d'une nouvelle réduction: {}", request.getIntitule());
        validateCible(request);
        Reduction reduction = mapToEntity(request);
        Reduction saved = reductionRepository.save(reduction);
        log.info("Réduction créée avec succès: ID={}", saved.getId());
        return mapToResponse(saved);
    }

    @Transactional(readOnly = true)
    public Page<ReductionResponse> getAll(Pageable pageable) {
        return reductionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public ReductionResponse getById(UUID id) {
        Reduction reduction = findReductionById(id);
        return mapToResponse(reduction);
    }

    @Transactional(readOnly = true)
    public Page<ReductionResponse> searchByIntitule(String intitule, Pageable pageable) {
        return reductionRepository.findByIntituleContainingIgnoreCase(intitule, pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public Page<ReductionResponse> getActives(Pageable pageable) {
        return reductionRepository.findByActifTrue(pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ReductionResponse> getValidReductions() {
        List<Reduction> reductions = reductionRepository.findValidReductions(LocalDate.now());
        return reductions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public Page<ReductionResponse> getExpired(Pageable pageable) {
        return reductionRepository.findExpired(LocalDate.now(), pageable)
                .map(this::mapToResponse);
    }

    @Transactional(readOnly = true)
    public List<ReductionResponse> getApplicableReductionsForLivre(UUID livreId) {
        Livre livre = livreRepository.findById(livreId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Livre non trouvé"));

        String categorieId = livre.getCategorie() != null ? livre.getCategorie().getId() : null;

        List<Reduction> reductions = reductionRepository.findApplicableReductionsOrdered(
                livreId.toString(),
                categorieId,
                LocalDate.now()
        );

        return reductions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ReductionResponse getBestReductionForLivre(UUID livreId) {
        log.debug("Recherche de la meilleure réduction pour le livre ID: {}", livreId);

        Livre livre = livreRepository.findById(livreId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Livre non trouvé avec ID: " + livreId));

        String categorieId = livre.getCategorie() != null ? livre.getCategorie().getId() : null;

        return reductionRepository.findBestApplicableReductionForLivre(
                livreId.toString(),
                categorieId,
                LocalDate.now()
            )
            .map(reduction -> {
                log.debug("Meilleure réduction trouvée : {} (valeur: {}, cible: {})",
                        reduction.getIntitule(), reduction.getValeur(), reduction.getCible());
                return mapToResponse(reduction);
            })
            .orElseGet(() -> {
                log.debug("Aucune réduction applicable pour le livre ID: {}", livreId);
                return null;
            });
    }

    public ReductionResponse update(UUID id, ReductionRequest request) {
        log.info("Mise à jour de la réduction: ID={}", id);
        Reduction reduction = findReductionById(id);
        validateCible(request);

        reduction.setIntitule(request.getIntitule());
        reduction.setDescription(request.getDescription());
        reduction.setType(request.getType());
        reduction.setValeur(request.getValeur());
        reduction.setCible(request.getCible());
        reduction.setCibleId(request.getCibleId() != null ? request.getCibleId().toString() : null);
        reduction.setDateDebut(request.getDateDebut());
        reduction.setDateFin(request.getDateFin());
        reduction.setActif(request.getActif());

        Reduction updated = reductionRepository.save(reduction);
        log.info("Réduction mise à jour avec succès: ID={}", id);
        return mapToResponse(updated);
    }

    public ReductionResponse toggleActif(UUID id) {
        log.info("Toggle actif pour la réduction: ID={}", id);
        Reduction reduction = findReductionById(id);
        reduction.setActif(!reduction.getActif());
        Reduction updated = reductionRepository.save(reduction);
        log.info("Réduction {} avec succès: ID={}", 
                updated.getActif() ? "activée" : "désactivée", id);
        return mapToResponse(updated);
    }

    public void delete(UUID id) {
        log.info("Suppression de la réduction: ID={}", id);
        Reduction reduction = findReductionById(id);
        reductionRepository.delete(reduction);
        log.info("Réduction supprimée avec succès: ID={}", id);
    }

    private Reduction findReductionById(UUID id) {
        return reductionRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réduction non trouvée avec l'ID: " + id));
    }

    private void validateCible(ReductionRequest request) {
        if (request.getCible() == CibleReduction.LIVRE && request.getCibleId() != null) {
            if (!livreRepository.existsById(request.getCibleId().toString())) {
                throw new BadRequestException("Le livre spécifié n'existe pas");
            }
        } else if (request.getCible() == CibleReduction.CATEGORIE && request.getCibleId() != null) {
            if (!categorieRepository.existsById(request.getCibleId().toString())) {
                throw new BadRequestException("La catégorie spécifiée n'existe pas");
            }
        }
    }

    private Reduction mapToEntity(ReductionRequest request) {
        Reduction reduction = new Reduction();
        reduction.setIntitule(request.getIntitule());
        reduction.setDescription(request.getDescription());
        reduction.setType(request.getType());
        reduction.setValeur(request.getValeur());
        reduction.setCible(request.getCible());
        reduction.setCibleId(request.getCibleId() != null ? request.getCibleId().toString() : null);
        reduction.setDateDebut(request.getDateDebut());
        reduction.setDateFin(request.getDateFin());
        reduction.setActif(request.getActif());
        return reduction;
    }

    private ReductionResponse mapToResponse(Reduction reduction) {
        String cibleNom = null;

        if (reduction.getCible() == CibleReduction.LIVRE && reduction.getCibleId() != null) {
            cibleNom = livreRepository.findById(reduction.getCibleId())
                    .map(Livre::getTitre)
                    .orElse(null);
        } else if (reduction.getCible() == CibleReduction.CATEGORIE && reduction.getCibleId() != null) {
            cibleNom = categorieRepository.findById(reduction.getCibleId())
                    .map(categorie -> categorie.getNom())
                    .orElse(null);
        }

        return ReductionResponse.builder()
                .id(reduction.getId())
                .intitule(reduction.getIntitule())
                .description(reduction.getDescription())
                .type(reduction.getType())
                .valeur(reduction.getValeur())
                .cible(reduction.getCible())
                .cibleId(reduction.getCibleId() != null ? reduction.getCibleId() : null)
                .cibleNom(cibleNom)
                .dateDebut(reduction.getDateDebut())
                .dateFin(reduction.getDateFin())
                .actif(reduction.getActif())
                .estValide(reduction.estValide())
                .createdAt(reduction.getCreatedAt())
                .updatedAt(reduction.getUpdatedAt())
                .build();
    }
}