package com.hexalib.api.commande.model;

import com.hexalib.api.fournisseur.model.Fournisseur;
import com.hexalib.api.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "commandes_fournisseurs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandeFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 30)
    private String numeroCommande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "fournisseur_id", nullable = false)
    private Fournisseur fournisseur;

    @Column(nullable = false)
    private LocalDate dateCommande;

    @Column
    private LocalDate dateReceptionPrevue;

    @Column
    private LocalDate dateReceptionReelle;

    @Column(precision = 10, scale = 2)
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.EN_ATTENTE;

    @Column(columnDefinition = "TEXT")
    private String notes;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @OneToMany(mappedBy = "commande", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneCommandeFournisseur> lignes = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Ajouter une ligne de commande
     */
    public void addLigne(LigneCommandeFournisseur ligne) {
        lignes.add(ligne);
        ligne.setCommande(this);
        calculateMontantTotal();
    }

    /**
     * Retirer une ligne de commande
     */
    public void removeLigne(LigneCommandeFournisseur ligne) {
        lignes.remove(ligne);
        ligne.setCommande(null);
        calculateMontantTotal();
    }

    /**
     * Calculer le montant total de la commande
     */
    public void calculateMontantTotal() {
        this.montantTotal = lignes.stream()
                .map(LigneCommandeFournisseur::getSousTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Marquer la commande comme reçue
     */
    public void marquerCommeRecue(LocalDate dateReception) {
        this.statut = Statut.RECUE;
        this.dateReceptionReelle = dateReception;
    }

    /**
     * Annuler la commande
     */
    public void annuler() {
        this.statut = Statut.ANNULEE;
    }

    public enum Statut {
        EN_ATTENTE,
        RECUE,
        ANNULEE
    }
}