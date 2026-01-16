package com.hexalib.api.reduction.service;

import com.hexalib.api.categorie.model.Categorie;
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

    /**
     * Créer une nouvelle réduction
     */
    public ReductionResponse create(ReductionRequest request) {
        log.info("Création d'une nouvelle réduction: {}", request.getIntitule());

        // Validation de la cible
        validateCible(request);

        Reduction reduction = mapTomodel(request);
        Reduction saved = reductionRepository.save(reduction);

        log.info("Réduction créée avec succès: ID={}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Récupérer toutes les réductions (paginées)
     */
    @Transactional(readOnly = true)
    public Page<ReductionResponse> getAll(Pageable pageable) {
        return reductionRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer une réduction par ID
     */
    @Transactional(readOnly = true)
    public ReductionResponse getById(UUID id) {
        Reduction reduction = findReductionById(id);
        return mapToResponse(reduction);
    }

    /**
     * Rechercher des réductions par intitulé
     */
    @Transactional(readOnly = true)
    public Page<ReductionResponse> searchByIntitule(String intitule, Pageable pageable) {
        return reductionRepository.findByIntituleContainingIgnoreCase(intitule, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les réductions actives
     */
    @Transactional(readOnly = true)
    public Page<ReductionResponse> getActives(Pageable pageable) {
        return reductionRepository.findByActifTrue(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les réductions valides (actives + période en cours)
     */
    @Transactional(readOnly = true)
    public List<ReductionResponse> getValidReductions() {
        List<Reduction> reductions = reductionRepository.findValidReductions(LocalDate.now());
        return reductions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer les réductions expirées
     */
    @Transactional(readOnly = true)
    public Page<ReductionResponse> getExpired(Pageable pageable) {
        return reductionRepository.findExpired(LocalDate.now(), pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les réductions applicables à un livre
     */
    @Transactional(readOnly = true)
    public List<ReductionResponse> getApplicableReductionsForLivre(UUID livreId) {
        // Conversion UUID -> String pour l'ID du livre
        Livre livre = livreRepository.findById(livreId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Livre non trouvé"));

        // Conversion de l'ID de la catégorie en UUID
        UUID categorieUuid = UUID.fromString(livre.getCategorie().getId());

        List<Reduction> reductions = reductionRepository.findApplicableReductions(
                livreId,
                categorieUuid,
                LocalDate.now()
        );

        return reductions.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Récupérer la meilleure réduction pour un livre
     */
    @Transactional(readOnly = true)
    public ReductionResponse getBestReductionForLivre(UUID livreId) {
        // Conversion UUID -> String pour l'ID du livre
        Livre livre = livreRepository.findById(livreId.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Livre non trouvé"));

        // Conversion de l'ID de la catégorie en UUID
        UUID categorieUuid = UUID.fromString(livre.getCategorie().getId());

        return reductionRepository.findBestReductionForLivre(
                        livreId,
                        categorieUuid,
                        LocalDate.now()
                )
                .map(this::mapToResponse)
                .orElse(null);
    }

    /**
     * Mettre à jour une réduction
     */
    public ReductionResponse update(UUID id, ReductionRequest request) {
        log.info("Mise à jour de la réduction: ID={}", id);

        Reduction reduction = findReductionById(id);

        // Validation de la cible
        validateCible(request);

        // Mise à jour des champs
        reduction.setIntitule(request.getIntitule());
        reduction.setDescription(request.getDescription());
        reduction.setType(request.getType());
        reduction.setValeur(request.getValeur());
        reduction.setCible(request.getCible());
        reduction.setCibleId(request.getCibleId());
        reduction.setDateDebut(request.getDateDebut());
        reduction.setDateFin(request.getDateFin());
        reduction.setActif(request.getActif());

        Reduction updated = reductionRepository.save(reduction);
        log.info("Réduction mise à jour avec succès: ID={}", id);

        return mapToResponse(updated);
    }

    /**
     * Activer/Désactiver une réduction
     */
    public ReductionResponse toggleActif(UUID id) {
        log.info("Toggle actif pour la réduction: ID={}", id);

        Reduction reduction = findReductionById(id);
        reduction.setActif(!reduction.getActif());

        Reduction updated = reductionRepository.save(reduction);
        log.info("Réduction {} avec succès: ID={}", 
                updated.getActif() ? "activée" : "désactivée", id);

        return mapToResponse(updated);
    }

    /**
     * Supprimer une réduction
     */
    public void delete(UUID id) {
        log.info("Suppression de la réduction: ID={}", id);

        Reduction reduction = findReductionById(id);

        // TODO: Vérifier si la réduction est utilisée dans des ventes
        // Si oui, on peut soit empêcher la suppression, soit faire une suppression logique

        reductionRepository.delete(reduction);
        log.info("Réduction supprimée avec succès: ID={}", id);
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private Reduction findReductionById(UUID id) {
        return reductionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Réduction non trouvée avec l'ID: " + id));
    }

    private void validateCible(ReductionRequest request) {
        if (request.getCible() == CibleReduction.LIVRE && request.getCibleId() != null) {
            // Conversion UUID -> String pour vérifier l'existence du livre
            if (!livreRepository.existsById(request.getCibleId().toString())) {
                throw new BadRequestException("Le livre spécifié n'existe pas");
            }
        } else if (request.getCible() == CibleReduction.CATEGORIE && request.getCibleId() != null) {
            // Conversion UUID -> String pour vérifier l'existence de la catégorie
            if (!categorieRepository.existsById(request.getCibleId().toString())) {
                throw new BadRequestException("La catégorie spécifiée n'existe pas");
            }
        }
    }

    private Reduction mapTomodel(ReductionRequest request) {
        Reduction reduction = new Reduction();
        reduction.setIntitule(request.getIntitule());
        reduction.setDescription(request.getDescription());
        reduction.setType(request.getType());
        reduction.setValeur(request.getValeur());
        reduction.setCible(request.getCible());
        reduction.setCibleId(request.getCibleId());
        reduction.setDateDebut(request.getDateDebut());
        reduction.setDateFin(request.getDateFin());
        reduction.setActif(request.getActif());
        return reduction;
    }

    private ReductionResponse mapToResponse(Reduction reduction) {
        String cibleNom = null;

        if (reduction.getCible() == CibleReduction.LIVRE && reduction.getLivre() != null) {
            cibleNom = reduction.getLivre().getTitre();
        } else if (reduction.getCible() == CibleReduction.CATEGORIE && reduction.getCategorie() != null) {
            cibleNom = reduction.getCategorie().getNom();
        }

        return ReductionResponse.builder()
                .id(reduction.getId())
                .intitule(reduction.getIntitule())
                .description(reduction.getDescription())
                .type(reduction.getType())
                .valeur(reduction.getValeur())
                .cible(reduction.getCible())
                .cibleId(reduction.getCibleId())
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