package com.hexalib.api.commande.service;

import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.commande.dto.CommandeFournisseurRequest;
import com.hexalib.api.commande.dto.CommandeFournisseurResponse;
import com.hexalib.api.commande.dto.LigneCommandeRequest;
import com.hexalib.api.commande.model.CommandeFournisseur;
import com.hexalib.api.commande.model.LigneCommandeFournisseur;
import com.hexalib.api.commande.repository.CommandeFournisseurRepository;
import com.hexalib.api.commande.repository.LigneCommandeFournisseurRepository;
import com.hexalib.api.common.dto.PageResponse;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.fournisseur.model.Fournisseur;
import com.hexalib.api.fournisseur.repository.FournisseurRepository;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommandeFournisseurService {

    private final CommandeFournisseurRepository commandeRepository;
    private final LigneCommandeFournisseurRepository ligneRepository;
    private final FournisseurRepository fournisseurRepository;
    private final LivreRepository livreRepository;
    private final UserRepository userRepository;

    @Transactional
    public CommandeFournisseurResponse createCommande(CommandeFournisseurRequest request) {
        // Vérifier que le fournisseur existe
        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", request.getFournisseurId()));

        // Générer le numéro de commande unique
        String numeroCommande = generateNumeroCommande();

        // Récupérer l'utilisateur connecté
        User currentUser = getCurrentUser();

        // Créer la commande
        CommandeFournisseur commande = new CommandeFournisseur();
        commande.setNumeroCommande(numeroCommande);
        commande.setFournisseur(fournisseur);
        commande.setDateCommande(request.getDateCommande());
        commande.setDateReceptionPrevue(request.getDateReceptionPrevue());
        commande.setNotes(request.getNotes());
        commande.setStatut(CommandeFournisseur.Statut.EN_ATTENTE);
        commande.setCreatedBy(currentUser);

        // Ajouter les lignes de commande
        for (LigneCommandeRequest ligneRequest : request.getLignes()) {
            Livre livre = livreRepository.findById(ligneRequest.getLivreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", ligneRequest.getLivreId()));

            LigneCommandeFournisseur ligne = new LigneCommandeFournisseur();
            ligne.setLivre(livre);
            ligne.setQuantite(ligneRequest.getQuantite());
            ligne.setPrixAchatUnitaire(ligneRequest.getPrixAchatUnitaire());
            ligne.calculateSousTotal();

            commande.addLigne(ligne);
        }

        // Calculer le montant total
        commande.calculateMontantTotal();

        // Sauvegarder
        CommandeFournisseur savedCommande = commandeRepository.save(commande);
        return CommandeFournisseurResponse.fromEntity(savedCommande);
    }

    public CommandeFournisseurResponse getCommandeById(String id) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", id));
        return CommandeFournisseurResponse.fromEntity(commande);
    }

    public PageResponse<CommandeFournisseurResponse> getAllCommandes(
            int page,
            int size,
            String search,
            String fournisseurId,
            String statut,
            LocalDate dateDebut,
            LocalDate dateFin
    ) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCommande").descending());

        Page<CommandeFournisseur> commandePage;

        // Recherche avec filtres combinés
        if ((search != null && !search.trim().isEmpty()) ||
            (fournisseurId != null && !fournisseurId.trim().isEmpty()) ||
            (statut != null && !statut.trim().isEmpty()) ||
            dateDebut != null || dateFin != null) {

            CommandeFournisseur.Statut statutEnum = null;
            if (statut != null && !statut.isEmpty()) {
                try {
                    statutEnum = CommandeFournisseur.Statut.valueOf(statut.toUpperCase());
                } catch (IllegalArgumentException e) {
                    throw new BadRequestException("Statut invalide: " + statut);
                }
            }

            commandePage = commandeRepository.searchWithFilters(
                search != null && !search.isEmpty() ? search : null,
                fournisseurId != null && !fournisseurId.isEmpty() ? fournisseurId : null,
                statutEnum,
                dateDebut,
                dateFin,
                pageable
            );
        } else {
            commandePage = commandeRepository.findAll(pageable);
        }

        List<CommandeFournisseurResponse> content = commandePage.getContent().stream()
                .map(CommandeFournisseurResponse::fromEntitySimple)
                .collect(Collectors.toList());

        return new PageResponse<>(
                content,
                commandePage.getNumber(),
                commandePage.getSize(),
                commandePage.getTotalElements(),
                commandePage.getTotalPages(),
                commandePage.isLast()
        );
    }

    @Transactional
    public CommandeFournisseurResponse updateCommande(String id, CommandeFournisseurRequest request) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", id));

        // Vérifier que la commande est en attente
        if (commande.getStatut() != CommandeFournisseur.Statut.EN_ATTENTE) {
            throw new BadRequestException("Seules les commandes en attente peuvent être modifiées");
        }

        // Vérifier que le fournisseur existe
        Fournisseur fournisseur = fournisseurRepository.findById(request.getFournisseurId())
                .orElseThrow(() -> new ResourceNotFoundException("Fournisseur", "id", request.getFournisseurId()));

        // Mettre à jour les informations de base
        commande.setFournisseur(fournisseur);
        commande.setDateCommande(request.getDateCommande());
        commande.setDateReceptionPrevue(request.getDateReceptionPrevue());
        commande.setNotes(request.getNotes());

        // Supprimer les anciennes lignes
        commande.getLignes().clear();

        // Ajouter les nouvelles lignes
        for (LigneCommandeRequest ligneRequest : request.getLignes()) {
            Livre livre = livreRepository.findById(ligneRequest.getLivreId())
                    .orElseThrow(() -> new ResourceNotFoundException("Livre", "id", ligneRequest.getLivreId()));

            LigneCommandeFournisseur ligne = new LigneCommandeFournisseur();
            ligne.setLivre(livre);
            ligne.setQuantite(ligneRequest.getQuantite());
            ligne.setPrixAchatUnitaire(ligneRequest.getPrixAchatUnitaire());
            ligne.calculateSousTotal();

            commande.addLigne(ligne);
        }

        // Recalculer le montant total
        commande.calculateMontantTotal();

        CommandeFournisseur updatedCommande = commandeRepository.save(commande);
        return CommandeFournisseurResponse.fromEntity(updatedCommande);
    }

    @Transactional
    public void deleteCommande(String id) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", id));

        // Vérifier que la commande est en attente
        if (commande.getStatut() != CommandeFournisseur.Statut.EN_ATTENTE) {
            throw new BadRequestException("Seules les commandes en attente peuvent être supprimées");
        }

        commandeRepository.delete(commande);
    }

    @Transactional
    public CommandeFournisseurResponse recevoirCommande(String id, LocalDate dateReception) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", id));

        // Vérifier que la commande est en attente
        if (commande.getStatut() != CommandeFournisseur.Statut.EN_ATTENTE) {
            throw new BadRequestException("Cette commande a déjà été traitée");
        }

        // Marquer comme reçue
        commande.marquerCommeRecue(dateReception);

        // Mettre à jour le stock de chaque livre
        for (LigneCommandeFournisseur ligne : commande.getLignes()) {
            Livre livre = ligne.getLivre();
            int nouvelleQuantite = livre.getQuantiteStock() + ligne.getQuantite();
            livre.setQuantiteStock(nouvelleQuantite);
            
            // Mettre à jour le prix d'achat du livre si fourni
            if (ligne.getPrixAchatUnitaire() != null) {
                livre.setPrixAchat(ligne.getPrixAchatUnitaire());
            }
            
            livreRepository.save(livre);

            // TODO: Créer un mouvement de stock "ENTREE"
        }

        CommandeFournisseur updatedCommande = commandeRepository.save(commande);
        return CommandeFournisseurResponse.fromEntity(updatedCommande);
    }

    @Transactional
    public CommandeFournisseurResponse annulerCommande(String id, String motif) {
        CommandeFournisseur commande = commandeRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commande", "id", id));

        // Vérifier que la commande est en attente
        if (commande.getStatut() != CommandeFournisseur.Statut.EN_ATTENTE) {
            throw new BadRequestException("Seules les commandes en attente peuvent être annulées");
        }

        // Annuler la commande
        commande.annuler();
        if (motif != null && !motif.isEmpty()) {
            commande.setNotes(commande.getNotes() != null 
                ? commande.getNotes() + "\n\nMotif d'annulation: " + motif 
                : "Motif d'annulation: " + motif);
        }

        CommandeFournisseur updatedCommande = commandeRepository.save(commande);
        return CommandeFournisseurResponse.fromEntity(updatedCommande);
    }

    /**
     * Générer un numéro de commande unique
     * Format: CMD-YYYYMMDD-XXX
     */
    private String generateNumeroCommande() {
        LocalDate today = LocalDate.now();
        String datePrefix = today.format(DateTimeFormatter.ofPattern("yyyyMMdd"));
        String baseNumero = "CMD-" + datePrefix;
        
        int counter = 1;
        String numeroCommande;
        
        do {
            numeroCommande = String.format("%s-%03d", baseNumero, counter);
            counter++;
            
            if (counter > 999) {
                throw new BadRequestException("Limite de commandes atteinte pour aujourd'hui");
            }
        } while (commandeRepository.existsByNumeroCommande(numeroCommande));
        
        return numeroCommande;
    }

    /**
     * Récupérer l'utilisateur connecté
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur", "email", email));
    }
}