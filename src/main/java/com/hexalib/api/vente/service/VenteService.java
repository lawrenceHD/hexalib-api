package com.hexalib.api.vente.service;

import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import com.hexalib.api.reduction.model.Reduction;
import com.hexalib.api.reduction.repository.ReductionRepository;
import com.hexalib.api.stock.model.MouvementStock;
import com.hexalib.api.stock.model.TypeMouvement;
import com.hexalib.api.stock.repository.MouvementStockRepository;
import com.hexalib.api.vente.dto.LigneVenteRequest;
import com.hexalib.api.vente.dto.LigneVenteResponse;
import com.hexalib.api.vente.dto.VenteRequest;
import com.hexalib.api.vente.dto.VenteResponse;
import com.hexalib.api.vente.model.LigneVente;
import com.hexalib.api.vente.model.StatutVente;
import com.hexalib.api.vente.model.Vente;
import com.hexalib.api.vente.repository.LigneVenteRepository;
import com.hexalib.api.vente.repository.VenteRepository;
import com.hexalib.api.vente.dto.GlobalStatsResponse;
import com.hexalib.api.vente.dto.VendeurStatsResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class VenteService {

    private final VenteRepository venteRepository;
    private final LigneVenteRepository ligneVenteRepository;
    private final LivreRepository livreRepository;
    private final ReductionRepository reductionRepository;
    private final UserRepository userRepository;
    private final MouvementStockRepository mouvementStockRepository;

    /**
     * Créer une nouvelle vente
     */
    public VenteResponse create(VenteRequest request) {
        log.info("Création d'une nouvelle vente avec {} lignes", request.getLignes().size());

        // Récupérer l'utilisateur connecté
        User vendeur = getCurrentUser();

        // Valider le stock avant de commencer
        validateStock(request.getLignes());

        // Créer la vente
        Vente vente = new Vente();
        vente.setNumeroFacture(generateNumeroFacture());
        vente.setDateVente(LocalDateTime.now());
        vente.setVendeur(vendeur);
        vente.setStatut(StatutVente.VALIDEE);

        // Calculer les montants
        BigDecimal montantHT = BigDecimal.ZERO;
        BigDecimal montantReductions = BigDecimal.ZERO;

        // Traiter chaque ligne
        for (LigneVenteRequest ligneReq : request.getLignes()) {
            LigneVente ligne = createLigneVente(ligneReq, vente);
            vente.addLigne(ligne);

            montantHT = montantHT.add(ligne.getPrixUnitaire().multiply(BigDecimal.valueOf(ligne.getQuantite())));
            montantReductions = montantReductions.add(ligne.getMontantReduction());

            // Déduire le stock
            deduireStock(ligne.getLivre(), ligne.getQuantite(), vente.getNumeroFacture(), vendeur);
        }

        vente.setMontantHT(montantHT);
        vente.setMontantReductions(montantReductions);
        vente.setMontantTTC(montantHT.subtract(montantReductions));

        // Sauvegarder
        Vente saved = venteRepository.save(vente);

        log.info("Vente créée avec succès: {}", saved.getNumeroFacture());
        return mapToResponse(saved);
    }

    /**
     * Récupérer toutes les ventes (paginées)
     */
    @Transactional(readOnly = true)
    public Page<VenteResponse> getAll(Pageable pageable) {
        return venteRepository.findAll(pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer une vente par ID
     */
    @Transactional(readOnly = true)
    public VenteResponse getById(UUID id) {
        Vente vente = findVenteById(id);
        return mapToResponse(vente);
    }

    /**
     * Rechercher des ventes
     */
    @Transactional(readOnly = true)
    public Page<VenteResponse> search(String search, Pageable pageable) {
        return venteRepository.search(search, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les ventes d'un vendeur
     */
    @Transactional(readOnly = true)
    public Page<VenteResponse> getByVendeur(String vendeurId, Pageable pageable) {
        return venteRepository.findByVendeurIdOrderByDateVenteDesc(vendeurId, pageable)
                .map(this::mapToResponse);
    }

    /**
     * Récupérer les ventes du vendeur connecté
     */
    @Transactional(readOnly = true)
    public Page<VenteResponse> getMesVentes(Pageable pageable) {
        User vendeur = getCurrentUser();
        return venteRepository.findByVendeurIdOrderByDateVenteDesc(vendeur.getId(), pageable)
                .map(this::mapToResponse);
    }

    /**
     * Annuler une vente (Admin uniquement)
     */
    public VenteResponse annuler(UUID id, String motif) {
        log.info("Annulation de la vente: ID={}", id);

        Vente vente = findVenteById(id);

        if (vente.getStatut() == StatutVente.ANNULEE) {
            throw new BadRequestException("Cette vente est déjà annulée");
        }

        User user = getCurrentUser();

        // Restaurer le stock
        for (LigneVente ligne : vente.getLignes()) {
            restaurerStock(ligne.getLivre(), ligne.getQuantite(), vente.getNumeroFacture(), user);
        }

        vente.setStatut(StatutVente.ANNULEE);
        vente.setMotifAnnulation(motif);

        Vente updated = venteRepository.save(vente);
        log.info("Vente annulée avec succès: {}", vente.getNumeroFacture());

        return mapToResponse(updated);
    }

    /**
     * Statistiques du jour pour un vendeur
     */
    @Transactional(readOnly = true)
    public VendeurStatsResponse getStatsVendeur(String vendeurId) {
        LocalDate today = LocalDate.now();
        
        long nombreVentes = venteRepository.countByVendeurAndDate(vendeurId, today);
        BigDecimal ca = venteRepository.sumMontantByVendeurAndDate(vendeurId, today);

        return VendeurStatsResponse.builder()
                .nombreVentes(nombreVentes)
                .chiffreAffaires(ca != null ? ca : BigDecimal.ZERO)
                .build();
    }

    /**
     * Statistiques du jour (global)
     */
    @Transactional(readOnly = true)
    public GlobalStatsResponse getStatsJour(LocalDate date) {
        long nombreVentes = venteRepository.countByDate(date);
        BigDecimal ca = venteRepository.sumMontantByDate(date);

        return GlobalStatsResponse.builder()
                .nombreVentes(nombreVentes)
                .chiffreAffaires(ca != null ? ca : BigDecimal.ZERO)
                .date(date)
                .build();
    }

    // ==================== MÉTHODES PRIVÉES ====================

   /**
 * Créer une ligne de vente avec application automatique de la meilleure réduction
 */
private LigneVente createLigneVente(LigneVenteRequest request, Vente vente) {
    log.debug("Création ligne vente pour livre ID: {}, quantité: {}", request.getLivreId(), request.getQuantite());

    Livre livre = livreRepository.findById(request.getLivreId().toString())
            .orElseThrow(() -> new ResourceNotFoundException(
                    "Livre non trouvé avec ID: " + request.getLivreId()));

    // Vérifier le stock disponible
    if (livre.getQuantiteStock() < request.getQuantite()) {
        throw new BadRequestException(
            String.format("Stock insuffisant pour '%s'. Disponible: %d, Demandé: %d",
                livre.getTitre(), livre.getQuantiteStock(), request.getQuantite())
        );
    }

    LigneVente ligne = new LigneVente();
    ligne.setVente(vente);
    ligne.setLivre(livre);
    ligne.setTitreLivre(livre.getTitre());
    ligne.setCodeLivre(livre.getCode());
    ligne.setPrixUnitaire(livre.getPrixVente());
    ligne.setQuantite(request.getQuantite());

    // Récupérer la meilleure réduction applicable (sûre même avec plusieurs résultats)
    String categorieId = livre.getCategorie() != null ? livre.getCategorie().getId() : null;

    Reduction meilleure = reductionRepository.findBestApplicableReductionForLivre(
            livre.getId(),
            categorieId,
            LocalDate.now()
    ).orElse(null);

    if (meilleure != null) {
        log.debug("Meilleure réduction appliquée: {} (type: {}, valeur: {})",
                meilleure.getIntitule(), meilleure.getType(), meilleure.getValeur());

        BigDecimal montantReduc = meilleure.calculerMontantReduction(
            ligne.getPrixUnitaire().multiply(BigDecimal.valueOf(ligne.getQuantite()))
        );

        ligne.setReduction(meilleure);
        ligne.setMontantReduction(montantReduc);
    } else {
        log.debug("Aucune réduction applicable pour le livre {}", livre.getTitre());
        ligne.setMontantReduction(BigDecimal.ZERO);
    }

    ligne.calculerSousTotal();
    return ligne;
}


    /**
     * Valider le stock avant création de vente
     */
    private void validateStock(List<LigneVenteRequest> lignes) {
        List<String> erreurs = new ArrayList<>();

        for (LigneVenteRequest ligne : lignes) {
            Livre livre = livreRepository.findById(ligne.getLivreId().toString())
                    .orElseThrow(() -> new ResourceNotFoundException("Livre non trouvé"));

            if (livre.getQuantiteStock() < ligne.getQuantite()) {
                erreurs.add(String.format("%s: stock insuffisant (disponible: %d)",
                        livre.getTitre(), livre.getQuantiteStock()));
            }
        }

        if (!erreurs.isEmpty()) {
            throw new BadRequestException("Erreurs de stock: " + String.join(", ", erreurs));
        }
    }

    /**
     * Déduire le stock et créer un mouvement
     */
    private void deduireStock(Livre livre, Integer quantite, String reference, User user) {
        int stockAvant = livre.getQuantiteStock();
        int stockApres = stockAvant - quantite;

        livre.setQuantiteStock(stockApres);
        livreRepository.save(livre);

        // Créer le mouvement de stock
        MouvementStock mouvement = new MouvementStock();
        mouvement.setLivre(livre);
        mouvement.setTypeMouvement(TypeMouvement.SORTIE);
        mouvement.setQuantite(-quantite);
        mouvement.setStockAvant(stockAvant);
        mouvement.setStockApres(stockApres);
        mouvement.setMotif("Vente");
        mouvement.setReference(reference);
        mouvement.setUser(user);
        mouvement.setDateMouvement(LocalDateTime.now());

        mouvementStockRepository.save(mouvement);
    }

    /**
     * Restaurer le stock (annulation)
     */
    private void restaurerStock(Livre livre, Integer quantite, String reference, User user) {
        int stockAvant = livre.getQuantiteStock();
        int stockApres = stockAvant + quantite;

        livre.setQuantiteStock(stockApres);
        livreRepository.save(livre);

        // Créer le mouvement de stock
        MouvementStock mouvement = new MouvementStock();
        mouvement.setLivre(livre);
        mouvement.setTypeMouvement(TypeMouvement.RETOUR);
        mouvement.setQuantite(quantite);
        mouvement.setStockAvant(stockAvant);
        mouvement.setStockApres(stockApres);
        mouvement.setMotif("Annulation vente");
        mouvement.setReference(reference);
        mouvement.setUser(user);
        mouvement.setDateMouvement(LocalDateTime.now());

        mouvementStockRepository.save(mouvement);
    }

    /**
     * Générer un numéro de facture unique
     */
    private String generateNumeroFacture() {
        String dateStr = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        long count = venteRepository.countByDate(LocalDate.now());
        return String.format("FAC-%s-%03d", dateStr, count + 1);
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
     * Trouver une vente par ID
     */
    private Vente findVenteById(UUID id) {
        return venteRepository.findById(id.toString())
                .orElseThrow(() -> new ResourceNotFoundException("Vente non trouvée avec l'ID: " + id));
    }

    /**
     * Mapper Vente vers VenteResponse
     */
    private VenteResponse mapToResponse(Vente vente) {
        List<LigneVenteResponse> lignesResponse = vente.getLignes().stream()
                .map(this::mapLigneToResponse)
                .collect(Collectors.toList());

        return VenteResponse.builder()
                .id(vente.getId())
                .numeroFacture(vente.getNumeroFacture())
                .dateVente(vente.getDateVente())
                .vendeurId(vente.getVendeur().getId())
                .vendeurNom(vente.getVendeur().getNomComplet())
                .montantHT(vente.getMontantHT())
                .montantReductions(vente.getMontantReductions())
                .montantTTC(vente.getMontantTTC())
                .statut(vente.getStatut())
                .motifAnnulation(vente.getMotifAnnulation())
                .lignes(lignesResponse)
                .createdAt(vente.getCreatedAt())
                .build();
    }

    /**
     * Mapper LigneVente vers LigneVenteResponse
     */
    private LigneVenteResponse mapLigneToResponse(LigneVente ligne) {
        return LigneVenteResponse.builder()
                .id(ligne.getId())
                .livreId(ligne.getLivre().getId())
                .titreLivre(ligne.getTitreLivre())
                .codeLivre(ligne.getCodeLivre())
                .prixUnitaire(ligne.getPrixUnitaire())
                .quantite(ligne.getQuantite())
                .reductionId(ligne.getReduction() != null ? ligne.getReduction().getId() : null)
                .reductionIntitule(ligne.getReduction() != null ? ligne.getReduction().getIntitule() : null)
                .montantReduction(ligne.getMontantReduction())
                .sousTotal(ligne.getSousTotal())
                .build();
    }
}