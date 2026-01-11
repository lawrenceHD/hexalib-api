package com.hexalib.api.commande.model;

import com.hexalib.api.livre.model.Livre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "lignes_commande_fournisseur")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneCommandeFournisseur {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "commande_id", nullable = false)
    private CommandeFournisseur commande;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livre_id", nullable = false)
    private Livre livre;

    @Column(nullable = false)
    private Integer quantite;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal prixAchatUnitaire;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal sousTotal;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    @PrePersist
    @PreUpdate
    public void calculateSousTotal() {
        if (quantite != null && prixAchatUnitaire != null) {
            this.sousTotal = prixAchatUnitaire.multiply(new BigDecimal(quantite));
        }
    }
}