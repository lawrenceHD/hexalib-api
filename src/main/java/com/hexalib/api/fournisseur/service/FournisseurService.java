package com.hexalib.api.fournisseur.service;

import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.fournisseur.dto.FournisseurRequest;
import com.hexalib.api.fournisseur.dto.FournisseurResponse;
import com.hexalib.api.fournisseur.model.Fournisseur;
import com.hexalib.api.fournisseur.repository.FournisseurRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FournisseurService {

    private final FournisseurRepository fournisseurRepository;

    @Transactional
    public FournisseurResponse createFournisseur(FournisseurRequest request) {
        // Vérifier si le nom existe déjà
        if (fournisseurRepository.existsByNom(request.getNom())) {
            throw new BadRequestException("Un fournisseur avec ce nom existe déjà");
        }

        // Vérifier si l'email existe déjà (si fourni)
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (fournisseurRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Un fournisseur avec cet email existe déjà");
            }
        }

        // Créer le fournisseur
        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setNom(request.getNom());
        fournisseur.setContact(request.getContact());
        fournisseur.setTelephone(request.getTelephone());
        fournisseur.setEmail(request.getEmail());
        fournisseur.setAdresse(request.getAdresse());
        fournisseur.setDelaiLivraisonJours(request.getDelaiLivraisonJours());
        fournisseur.setStatut(Fournisseur.Statut.ACTIF);

        Fournisseur savedFournisseur = fournisseurRepository.save(fournisseur);
        return FournisseurResponse.fromEntity(savedFournisseur);
    }

    public FournisseurResponse getFournisseurById(String id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", id));
        return FournisseurResponse.fromEntity(fournisseur);
    }

    public PageResponse<FournisseurResponse> getAllFournisseurs(
            int page, 
            int size, 
            String search,
            String statut
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Fournisseur> fournisseurPage;
        
        // Recherche avec filtres
        if ((search != null && !search.trim().isEmpty()) ||
            (statut != null && !statut.trim().isEmpty())) {
            
            Fournisseur.Statut statutEnum = null;
            if (statut != null && !statut.isEmpty()) {
                try {
                    statutEnum = Fournisseur.Statut.valueOf(statut.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Statut invalide: " + statut);
                }
            }
            
            fournisseurPage = fournisseurRepository.searchWithFilters(
                search != null && !search.isEmpty() ? search : null,
                statutEnum,
                pageable
            );
        } else {
            fournisseurPage = fournisseurRepository.findAll(pageable);
        }

        List<FournisseurResponse> content = fournisseurPage.getContent().stream()
                .map(FournisseurResponse::fromEntitySimple)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                fournisseurPage.getNumber(),
                fournisseurPage.getSize(),
                fournisseurPage.getTotalElements(),
                fournisseurPage.getTotalPages(),
                fournisseurPage.isLast()
        );
    }

    public List<FournisseurResponse> getAllFournisseursActifs() {
        return fournisseurRepository.findByStatut(Fournisseur.Statut.ACTIF, Pageable.unpaged())
                .stream()
                .map(FournisseurResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    @Transactional
    public FournisseurResponse updateFournisseur(String id, FournisseurRequest request) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", id));

        // Vérifier si le nouveau nom existe déjà (sauf pour ce fournisseur)
        if (!fournisseur.getNom().equals(request.getNom()) && 
            fournisseurRepository.existsByNomAndIdNot(request.getNom(), id)) {
            throw new BadRequestException("Un fournisseur avec ce nom existe déjà");
        }

        // Vérifier l'email si modifié
        if (request.getEmail() != null && !request.getEmail().isEmpty()) {
            if (!request.getEmail().equals(fournisseur.getEmail()) && 
                fournisseurRepository.existsByEmailAndIdNot(request.getEmail(), id)) {
                throw new BadRequestException("Un fournisseur avec cet email existe déjà");
            }
        }

        // Mettre à jour les champs
        fournisseur.setNom(request.getNom());
        fournisseur.setContact(request.getContact());
        fournisseur.setTelephone(request.getTelephone());
        fournisseur.setEmail(request.getEmail());
        fournisseur.setAdresse(request.getAdresse());
        fournisseur.setDelaiLivraisonJours(request.getDelaiLivraisonJours());

        Fournisseur updatedFournisseur = fournisseurRepository.save(fournisseur);
        return FournisseurResponse.fromEntity(updatedFournisseur);
    }

    @Transactional
    public void deleteFournisseur(String id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", id));

        // TODO: Vérifier si des commandes sont associées à ce fournisseur
        // Si oui, désactiver au lieu de supprimer
        
        fournisseurRepository.delete(fournisseur);
    }

    @Transactional
    public FournisseurResponse toggleStatut(String id) {
        Fournisseur fournisseur = fournisseurRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", id));

        if (fournisseur.getStatut() == Fournisseur.Statut.ACTIF) {
            fournisseur.setStatut(Fournisseur.Statut.INACTIF);
        } else {
            fournisseur.setStatut(Fournisseur.Statut.ACTIF);
        }

        Fournisseur updatedFournisseur = fournisseurRepository.save(fournisseur);
        return FournisseurResponse.fromEntity(updatedFournisseur);
    }
}