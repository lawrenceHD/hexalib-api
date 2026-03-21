package com.hexalib.api.comptabilite.service;

import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.comptabilite.dto.DepenseRequest;
import com.hexalib.api.comptabilite.dto.DepenseResponse;
import com.hexalib.api.comptabilite.model.CategorieDepense;
import com.hexalib.api.comptabilite.model.Depense;
import com.hexalib.api.comptabilite.repository.CategorieDepenseRepository;
import com.hexalib.api.comptabilite.repository.DepenseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class DepenseService {

    private final DepenseRepository           depenseRepository;
    private final CategorieDepenseRepository  categorieRepository;
    private final UserRepository              userRepository;

    public DepenseResponse create(DepenseRequest request) {
        CategorieDepense categorie = categorieRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie de dépense non trouvée"));

        if (categorie.getStatut() == CategorieDepense.Statut.INACTIF) {
            throw new BadRequestException("Cette catégorie de dépense est inactive");
        }

        User user = getCurrentUser();

        Depense depense = new Depense();
        depense.setDescription(request.getDescription());
        depense.setMontant(request.getMontant());
        depense.setDateDepense(request.getDateDepense());
        depense.setCategorie(categorie);
        depense.setReference(request.getReference());
        depense.setEnregistrePar(user);

        return DepenseResponse.fromEntity(depenseRepository.save(depense));
    }

    @Transactional(readOnly = true)
    public PageResponse<DepenseResponse> getAll(
            int page, int size,
            String categorieId,
            LocalDate debut,
            LocalDate fin) {

        Pageable pageable = PageRequest.of(page, size, Sort.by("dateDepense").descending());
        Page<Depense> result = depenseRepository.findWithFilters(
                categorieId, debut, fin, pageable);

        List<DepenseResponse> content = result.getContent().stream()
                .map(DepenseResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(content, result.getNumber(), result.getSize(),
                result.getTotalElements(), result.getTotalPages(), result.isLast());
    }

    @Transactional(readOnly = true)
    public DepenseResponse getById(String id) {
        return DepenseResponse.fromEntity(findById(id));
    }

    public DepenseResponse update(String id, DepenseRequest request) {
        Depense depense = findById(id);

        CategorieDepense categorie = categorieRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie de dépense non trouvée"));

        depense.setDescription(request.getDescription());
        depense.setMontant(request.getMontant());
        depense.setDateDepense(request.getDateDepense());
        depense.setCategorie(categorie);
        depense.setReference(request.getReference());

        return DepenseResponse.fromEntity(depenseRepository.save(depense));
    }

    public void delete(String id) {
        depenseRepository.delete(findById(id));
    }

    private Depense findById(String id) {
        return depenseRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Dépense non trouvée"));
    }

    private User getCurrentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return userRepository.findByEmail(auth.getName())
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }
}