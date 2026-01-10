package com.hexalib.api.livre.model;

import com.hexalib.api.categorie.model.Categorie;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "livres")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Livre {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 20)
    private String code;

    @Column(nullable = false, length = 255)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, length = 100)
    private String auteur;

    @Column(nullable = false, length = 100)
    private String maisonEdition;

    @Column
    private LocalDate dateParution;

    @Column(unique = true, length = 20)
    private String isbn;

    @Column(nullable = false, length = 50)
    private String langue;

    @Column(nullable = false)
    private Integer quantiteStock = 0;

    @Column(nullable = false)
    private Integer seuilMinimal = 5;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixVente;

    @Column(precision = 10, scale = 2)
    private BigDecimal prixAchat;

    @Column(length = 50)
    private String emplacement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "categorie_id", nullable = false)
    private Categorie categorie;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Statut statut = Statut.ACTIF;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(nullable = false)
    private LocalDateTime updatedAt = LocalDateTime.now();

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * Vérifie si le stock est critique (inférieur ou égal au seuil minimal)
     */
    @Transient
    public boolean isStockCritique() {
        return this.quantiteStock <= this.seuilMinimal;
    }

    /**
     * Vérifie si le livre est en rupture de stock
     */
    @Transient
    public boolean isEnRupture() {
        return this.quantiteStock == 0;
    }

    /**
     * Calcule la marge si le prix d'achat est renseigné
     */
    @Transient
    public BigDecimal getMarge() {
        if (this.prixAchat != null && this.prixVente != null) {
            return this.prixVente.subtract(this.prixAchat);
        }
        return null;
    }

    public enum Statut {
        ACTIF, INACTIF
    }

    public enum StatutStock {
        DISPONIBLE,      // Stock > seuil
        STOCK_CRITIQUE,  // Stock <= seuil
        RUPTURE         // Stock = 0
    }

    /**
     * Retourne le statut du stock actuel
     */
    @Transient
    public StatutStock getStatutStock() {
        if (this.quantiteStock == 0) {
            return StatutStock.RUPTURE;
        } else if (this.quantiteStock <= this.seuilMinimal) {
            return StatutStock.STOCK_CRITIQUE;
        } else {
            return StatutStock.DISPONIBLE;
        }
    }
}