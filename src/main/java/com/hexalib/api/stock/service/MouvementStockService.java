package com.hexalib.api.stock.service;

import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import com.hexalib.api.stock.dto.MouvementStockRequest;
import com.hexalib.api.stock.dto.MouvementStockResponse;
import com.hexalib.api.stock.model.MouvementStock;
import com.hexalib.api.stock.model.TypeMouvement;
import com.hexalib.api.stock.repository.MouvementStockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class MouvementStockService {

    private final MouvementStockRepository mouvementStockRepository;
    private final LivreRepository livreRepository;
    private final UserRepository userRepository;

    /**
     * Créer un mouvement de stock manuel (Admin uniquement)
     */
    public MouvementStockResponse create(MouvementStockRequest request) {
        log.info("Création d'un mouvement de stock: {} pour livre {}", 
                request.getTypeMouvement(), request.getLivreId());

        Livre livre = livreRepository.findById(request.getLivreId().toString())
                .orElseThrow(() -> new ResourceNotFoundException("Livre non trouvé"));

        User user = getCurrentUser();

        // Validation selon le type de mouvement
        validateMouvement(request, livre);

        int stockAvant = livre.getQuantiteStock();
        int quantite = request.getQuantite();
        int stockApres = calculerNouveauStock(request.getTypeMouvement(), stockAvant, quantite);

        // Vérifier que le stock ne devient pas négatif
        if (stockApres < 0) {
            throw new BadRequestException("Le stock ne peut pas être négatif");
        }

        // Créer le mouvement
        MouvementStock mouvement = new MouvementStock();
        mouvement.setLivre(livre);
        mouvement.setTypeMouvement(request.getTypeMouvement());
        mouvement.setQuantite(getQuantiteSignee(request.getTypeMouvement(), quantite));
        mouvement.setStockAvant(stockAvant);
        mouvement.setStockApres(stockApres);
        mouvement.setMotif(request.getMotif());
        mouvement.setReference(request.getReference());
        mouvement.setUser(user);
        mouvement.setDateMouvement(LocalDateTime.now());

        MouvementStock saved = mouvementStockRepository.save(mouvement);

        // Mettre à jour le stock du livre
        livre.setQuantiteStock(stockApres);
        livreRepository.save(livre);

        log.info("Mouvement de stock créé avec succès: ID={}", saved.getId());
        return mapToResponse(saved);
    }

    /**
     * Récupérer tous les mouvements (paginés)
     */
    @Transactional(readOnly = true)
    public Page<MouvementStockResponse> getAll(Pageable pageable) {
        return mouvementStockRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les mouvements d'un livre
     */
    @Transactional(readOnly = true)
    public Page<MouvementStockResponse> getByLivre(UUID livreId, Pageable pageable) {
        return mouvementStockRepository.findByLivreIdOrderByDateMouvementDesc(
                livreId.toString(), pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les mouvements par type
     */
    @Transactional(readOnly = true)
    public Page<MouvementStockResponse> getByType(TypeMouvement type, Pageable pageable) {
        return mouvementStockRepository.findByTypeMouvementOrderByDateMouvementDesc(type, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les mouvements avec filtres
     */
    @Transactional(readOnly = true)
    public Page<MouvementStockResponse> getWithFilters(
            String livreId,
            TypeMouvement type,
            String userId,
            LocalDateTime debut,
            LocalDateTime fin,
            Pageable pageable) {

        return mouvementStockRepository.findWithFilters(
                livreId, type, userId, debut, fin, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer l'historique complet d'un livre
     */
    @Transactional(readOnly = true)
    public List<MouvementStockResponse> getHistoriqueLivre(UUID livreId) {
        List<MouvementStock> mouvements = 
                mouvementStockRepository.findByLivreIdOrderByDateMouvementDesc(livreId.toString());
        
        return mouvements.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Valider un mouvement de stock
     */
    private void validateMouvement(MouvementStockRequest request, Livre livre) {
        TypeMouvement type = request.getTypeMouvement();
        int quantite = request.getQuantite();
        int stockActuel = livre.getQuantiteStock();

        if (type == TypeMouvement.SORTIE && quantite > stockActuel) {
            throw new BadRequestException(
                String.format("Stock insuffisant. Disponible: %d, Demandé: %d", 
                    stockActuel, quantite)
            );
        }

        if (type == TypeMouvement.AJUSTEMENT) {
            // Pour un ajustement, la quantité peut être négative (correction)
            int nouveauStock = stockActuel + quantite;
            if (nouveauStock < 0) {
                throw new BadRequestException("L'ajustement rendrait le stock négatif");
            }
        }
    }

    /**
     * Calculer le nouveau stock selon le type de mouvement
     */
    private int calculerNouveauStock(TypeMouvement type, int stockActuel, int quantite) {
        switch (type) {
            case ENTREE:
            case RETOUR:
                return stockActuel + quantite;
            case SORTIE:
                return stockActuel - quantite;
            case AJUSTEMENT:
                // Pour ajustement, la quantité contient déjà le signe
                return stockActuel + quantite;
            default:
                throw new IllegalArgumentException("Type de mouvement inconnu: " + type);
        }
    }

    /**
     * Obtenir la quantité signée pour l'enregistrement
     */
    private int getQuantiteSignee(TypeMouvement type, int quantite) {
        switch (type) {
            case ENTREE:
            case RETOUR:
                return quantite; // Positif
            case SORTIE:
                return -quantite; // Négatif
            case AJUSTEMENT:
                return quantite; // Peut être positif ou négatif
            default:
                return quantite;
        }
    }

    /**
     * Récupérer l'utilisateur connecté
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    /**
     * Mapper vers Response
     */
    private MouvementStockResponse mapToResponse(MouvementStock mouvement) {
        return MouvementStockResponse.builder()
                .id(mouvement.getId())
                .livreId(UUID.fromString(mouvement.getLivre().getId()))
                .titreLivre(mouvement.getLivre().getTitre())
                .codeLivre(mouvement.getLivre().getCode())
                .typeMouvement(mouvement.getTypeMouvement())
                .quantite(mouvement.getQuantite())
                .stockAvant(mouvement.getStockAvant())
                .stockApres(mouvement.getStockApres())
                .motif(mouvement.getMotif())
                .reference(mouvement.getReference())
                .userId(mouvement.getUser() != null ? UUID.fromString(mouvement.getUser().getId()) : null)
                .userName(mouvement.getUser() != null ? mouvement.getUser().getNomComplet() : null)
                .dateMouvement(mouvement.getDateMouvement())
                .build();
    }
}