package com.hexalib.api.vente.model;

import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.reduction.model.Reduction;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "lignes_vente")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LigneVente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vente_id", nullable = false)
    private Vente vente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livre_id", nullable = false)
    private Livre livre;

    @Column(name = "titre_livre", nullable = false)
    private String titreLivre;

    @Column(name = "code_livre", nullable = false, length = 20)
    private String codeLivre;

    @Column(name = "prix_unitaire", nullable = false, precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(nullable = false)
    private Integer quantite;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reduction_id")
    private Reduction reduction;

    @Column(name = "montant_reduction", precision = 10, scale = 2)
    private BigDecimal montantReduction = BigDecimal.ZERO;

    @Column(name = "sous_total", nullable = false, precision = 10, scale = 2)
    private BigDecimal sousTotal;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Méthode de calcul du sous-total
    public void calculerSousTotal() {
        BigDecimal total = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        this.sousTotal = total.subtract(montantReduction);
    }
}