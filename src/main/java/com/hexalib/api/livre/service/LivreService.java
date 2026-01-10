package com.hexalib.api.livre.service;

import com.hexalib.api.categorie.model.Categorie;
import com.hexalib.api.categorie.repository.CategorieRepository;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.livre.dto.LivreRequest;
import com.hexalib.api.livre.dto.LivreResponse;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
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
public class LivreService {

    private final LivreRepository livreRepository;
    private final CategorieRepository categorieRepository;

    @Transactional
    public LivreResponse createLivre(LivreRequest request) {
        // Vérifier si la catégorie existe
        Categorie categorie = categorieRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", request.getCategorieId()));

        // Vérifier si un livre avec même titre et auteur existe déjà
        if (livreRepository.existsByTitreAndAuteur(request.getTitre(), request.getAuteur())) {
            throw new BadRequestException("Un livre avec ce titre et cet auteur existe déjà");
        }

        // Vérifier l'ISBN si fourni
        if (request.getIsbn() != null && !request.getIsbn().isEmpty()) {
            if (livreRepository.existsByIsbn(request.getIsbn())) {
                throw new BadRequestException("Un livre avec cet ISBN existe déjà");
            }
        }

        // Générer un code unique basé sur la catégorie
        String code = generateUniqueLivreCode(categorie);

        // Créer le livre
        Livre livre = new Livre();
        livre.setCode(code);
        livre.setTitre(request.getTitre());
        livre.setDescription(request.getDescription());
        livre.setAuteur(request.getAuteur());
        livre.setMaisonEdition(request.getMaisonEdition());
        livre.setDateParution(request.getDateParution());
        livre.setIsbn(request.getIsbn());
        livre.setLangue(request.getLangue());
        livre.setQuantiteStock(request.getQuantiteStock());
        livre.setSeuilMinimal(request.getSeuilMinimal());
        livre.setPrixVente(request.getPrixVente());
        livre.setPrixAchat(request.getPrixAchat());
        livre.setEmplacement(request.getEmplacement());
        livre.setCategorie(categorie);
        livre.setStatut(Livre.Statut.ACTIF);

        Livre savedLivre = livreRepository.save(livre);
        return LivreResponse.fromEntity(savedLivre);
    }

    public LivreResponse getLivreById(String id) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", id));
        return LivreResponse.fromEntity(livre);
    }

    public PageResponse<LivreResponse> getAllLivres(
            int page, 
            int size, 
            String search,
            String categorieId,
            String statut,
            String langue
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        
        Page<Livre> livrePage;
        
        // Recherche avec filtres combinés
        if ((search != null && !search.trim().isEmpty()) ||
            (categorieId != null && !categorieId.trim().isEmpty()) ||
            (statut != null && !statut.trim().isEmpty()) ||
            (langue != null && !langue.trim().isEmpty())) {
            
            Livre.Statut statutEnum = null;
            if (statut != null && !statut.isEmpty()) {
                try {
                    statutEnum = Livre.Statut.valueOf(statut.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Statut invalide: " + statut);
                }
            }
            
            livrePage = livreRepository.searchWithFilters(
                search != null && !search.isEmpty() ? search : null,
                categorieId != null && !categorieId.isEmpty() ? categorieId : null,
                statutEnum,
                langue != null && !langue.isEmpty() ? langue : null,
                pageable
            );
        } else {
            livrePage = livreRepository.findAll(pageable);
        }

        List<LivreResponse> content = livrePage.getContent().stream()
                .map(LivreResponse::fromEntitySimple)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                livrePage.getNumber(),
                livrePage.getSize(),
                livrePage.getTotalElements(),
                livrePage.getTotalPages(),
                livrePage.isLast()
        );
    }

    public List<LivreResponse> getLivresStockCritique() {
        return livreRepository.findLivresStockCritique().stream()
                .map(LivreResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    public List<LivreResponse> getLivresEnRupture() {
        return livreRepository.findLivresEnRupture().stream()
                .map(LivreResponse::fromEntitySimple)
                .collect(Collectors.toList());
    }

    public List<String> getAllLangues() {
        return livreRepository.findAllLangues();
    }

    @Transactional
    public LivreResponse updateLivre(String id, LivreRequest request) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", id));

        // Vérifier si la catégorie existe
        Categorie categorie = categorieRepository.findById(request.getCategorieId())
                .orElseThrow(() -> new ResourceNotFoundException("Catégorie", "id", request.getCategorieId()));

        // Vérifier si titre/auteur existe déjà (sauf pour ce livre)
        if (livreRepository.existsByTitreAndAuteurAndIdNot(request.getTitre(), request.getAuteur(), id)) {
            throw new BadRequestException("Un livre avec ce titre et cet auteur existe déjà");
        }

        // Vérifier l'ISBN si modifié
        if (request.getIsbn() != null && !request.getIsbn().isEmpty()) {
            if (!request.getIsbn().equals(livre.getIsbn()) && 
                livreRepository.existsByIsbn(request.getIsbn())) {
                throw new BadRequestException("Un livre avec cet ISBN existe déjà");
            }
        }

        // Si la catégorie change, régénérer le code
        if (!livre.getCategorie().getId().equals(categorie.getId())) {
            String newCode = generateUniqueLivreCode(categorie);
            livre.setCode(newCode);
        }

        // Mettre à jour les champs
        livre.setTitre(request.getTitre());
        livre.setDescription(request.getDescription());
        livre.setAuteur(request.getAuteur());
        livre.setMaisonEdition(request.getMaisonEdition());
        livre.setDateParution(request.getDateParution());
        livre.setIsbn(request.getIsbn());
        livre.setLangue(request.getLangue());
        livre.setQuantiteStock(request.getQuantiteStock());
        livre.setSeuilMinimal(request.getSeuilMinimal());
        livre.setPrixVente(request.getPrixVente());
        livre.setPrixAchat(request.getPrixAchat());
        livre.setEmplacement(request.getEmplacement());
        livre.setCategorie(categorie);

        Livre updatedLivre = livreRepository.save(livre);
        return LivreResponse.fromEntity(updatedLivre);
    }

    @Transactional
    public void deleteLivre(String id) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", id));

        // TODO: Vérifier si des ventes sont associées à ce livre
        // Si oui, désactiver au lieu de supprimer
        
        livreRepository.delete(livre);
    }

    @Transactional
    public LivreResponse toggleStatut(String id) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", id));

        if (livre.getStatut() == Livre.Statut.ACTIF) {
            livre.setStatut(Livre.Statut.INACTIF);
        } else {
            livre.setStatut(Livre.Statut.ACTIF);
        }

        Livre updatedLivre = livreRepository.save(livre);
        return LivreResponse.fromEntity(updatedLivre);
    }

    @Transactional
    public LivreResponse ajusterStock(String id, Integer quantite, String motif) {
        Livre livre = livreRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", id));

        if (quantite < 0) {
            throw new BadRequestException("La quantité ne peut pas être négative");
        }

        livre.setQuantiteStock(quantite);
        
        // TODO: Créer un mouvement de stock avec le motif
        
        Livre updatedLivre = livreRepository.save(livre);
        return LivreResponse.fromEntity(updatedLivre);
    }

    /**
     * Génère un code unique pour le livre basé sur le code de la catégorie
     * Format: CODECAT-XXX (ex: ROMA123-001)
     */
    private String generateUniqueLivreCode(Categorie categorie) {
        String baseCode = categorie.getCode();
        String code;
        int attempts = 0;
        int counter = 1;
        
        do {
            code = String.format("%s-%03d", baseCode, counter);
            counter++;
            attempts++;
            
            if (attempts > 1000) {
                throw new BadRequestException("Impossible de générer un code unique pour ce livre");
            }
        } while (livreRepository.existsByCode(code));
        
        return code;
    }
}