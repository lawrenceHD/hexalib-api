package com.hexalib.api.comptabilite.service;

import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.comptabilite.dto.CategorieDepenseRequest;
import com.hexalib.api.comptabilite.dto.CategorieDepenseResponse;
import com.hexalib.api.comptabilite.model.CategorieDepense;
import com.hexalib.api.comptabilite.repository.CategorieDepenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CategorieDepenseService {

    private final CategorieDepenseRepository repository;

    public CategorieDepenseResponse create(CategorieDepenseRequest request) {
        if (repository.existsByNom(request.getNom())) {
            throw new BadRequestException("Une catégorie avec ce nom existe déjà");
        }
        CategorieDepense cat = new CategorieDepense();
        cat.setNom(request.getNom());
        cat.setDescription(request.getDescription());
        cat.setStatut(CategorieDepense.Statut.ACTIF);
        return CategorieDepenseResponse.fromEntity(repository.save(cat));
    }

    @Transactional(readOnly = true)
    public List<CategorieDepenseResponse> getAll() {
        return repository.findAll().stream()
                .map(CategorieDepenseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<CategorieDepenseResponse> getAllActives() {
        return repository.findByStatut(CategorieDepense.Statut.ACTIF).stream()
                .map(CategorieDepenseResponse::fromEntity)
                .collect(Collectors.toList());
    }

    public CategorieDepenseResponse update(String id, CategorieDepenseRequest request) {
        CategorieDepense cat = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie de dépense non trouvée"));
        if (repository.existsByNomAndIdNot(request.getNom(), id)) {
            throw new BadRequestException("Une catégorie avec ce nom existe déjà");
        }
        cat.setNom(request.getNom());
        cat.setDescription(request.getDescription());
        return CategorieDepenseResponse.fromEntity(repository.save(cat));
    }

    public CategorieDepenseResponse toggleStatut(String id) {
        CategorieDepense cat = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie de dépense non trouvée"));
        cat.setStatut(cat.getStatut() == CategorieDepense.Statut.ACTIF
                ? CategorieDepense.Statut.INACTIF
                : CategorieDepense.Statut.ACTIF);
        return CategorieDepenseResponse.fromEntity(repository.save(cat));
    }

    public void delete(String id) {
        CategorieDepense cat = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie de dépense non trouvée"));
        repository.delete(cat);
    }
}