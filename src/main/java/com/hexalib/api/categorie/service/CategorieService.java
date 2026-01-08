package com.hexalib.api.categorie.service;

import com.hexalib.api.categorie.dto.CategorieRequest;
import com.hexalib.api.categorie.dto.CategorieResponse;
import com.hexalib.api.categorie.model.Categorie;
import com.hexalib.api.categorie.repository.CategorieRepository;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.common.util.CodeGenerator;
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
public class CategorieService {

    private final CategorieRepository categorieRepository;

    @Transactional
    public CategorieResponse createCategorie(CategorieRequest request) {
        // Vérifier si le nom existe déjà
        if (categorieRepository.existsByNom(request.getNom())) {
            throw new BadRequestException("Une catégorie avec ce nom existe déjà");
        }

        // Générer un code unique
        String code = generateUniqueCode(request.getNom());

        // Créer la catégorie
        Categorie categorie = new Categorie();
        categorie.setNom(request.getNom());
        categorie.setDescription(request.getDescription());
        categorie.setCode(code);
        categorie.setStatut(Categorie.Statut.ACTIF);

        Categorie savedCategorie = categorieRepository.save(categorie);
        return CategorieResponse.fromEntity(savedCategorie);
    }

    public CategorieResponse getCategorieById(String id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", id));
        return CategorieResponse.fromEntity(categorie);
    }

    public PageResponse<CategorieResponse> getAllCategories(int page, int size, String search) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Categorie> categoriePage;
        if (search != null && !search.trim().isEmpty()) {
            categoriePage = categorieRepository.searchCategories(search, pageable);
        } else {
            categoriePage = categorieRepository.findAll(pageable);
        }

        List<CategorieResponse> content = categoriePage.getContent().stream()
                .map(CategorieResponse::fromEntity)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                categoriePage.getNumber(),
                categoriePage.getSize(),
                categoriePage.getTotalElements(),
                categoriePage.getTotalPages(),
                categoriePage.isLast()
        );
    }

    public List<CategorieResponse> getAllCategoriesActives() {
        return categorieRepository.findByStatut(Categorie.Statut.ACTIF, Pageable.unpaged())
                .stream()
                .map(CategorieResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategorieResponse updateCategorie(String id, CategorieRequest request) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", id));

        // Vérifier si le nouveau nom existe déjà (sauf pour cette catégorie)
        if (!categorie.getNom().equals(request.getNom()) && 
            categorieRepository.existsByNomAndIdNot(request.getNom(), id)) {
            throw new BadRequestException("Une catégorie avec ce nom existe déjà");
        }

        // Si le nom change, générer un nouveau code
        if (!categorie.getNom().equals(request.getNom())) {
            String newCode = generateUniqueCode(request.getNom());
            categorie.setCode(newCode);
        }

        categorie.setNom(request.getNom());
        categorie.setDescription(request.getDescription());

        Categorie updatedCategorie = categorieRepository.save(categorie);
        return CategorieResponse.fromEntity(updatedCategorie);
    }

    @Transactional
    public void deleteCategorie(String id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", id));

        // TODO: Vérifier si des livres sont associés à cette catégorie
        // Si oui, désactiver au lieu de supprimer
        
        categorieRepository.delete(categorie);
    }

    @Transactional
    public CategorieResponse toggleStatut(String id) {
        Categorie categorie = categorieRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", id));

        if (categorie.getStatut() == Categorie.Statut.ACTIF) {
            categorie.setStatut(Categorie.Statut.INACTIF);
        } else {
            categorie.setStatut(Categorie.Statut.ACTIF);
        }

        Categorie updatedCategorie = categorieRepository.save(categorie);
        return CategorieResponse.fromEntity(updatedCategorie);
    }

    private String generateUniqueCode(String nom) {
        String code;
        int attempts = 0;
        do {
            code = CodeGenerator.generateCategorieCode(nom);
            attempts++;
            if (attempts > 100) {
                throw new BadRequestException("Impossible de générer un code unique");
            }
        } while (categorieRepository.existsByCode(code));
        
        return code;
    }
}