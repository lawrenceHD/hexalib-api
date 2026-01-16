package com.hexalib.api.reduction.model;

import com.hexalib.api.categorie.model.Categorie;
import com.hexalib.api.livre.model.Livre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "reductions", indexes = {
    @Index(name = "idx_dates", columnList = "date_debut, date_fin, actif")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Reduction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 100)
    private String intitule;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TypeReduction type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CibleReduction cible;

    @Column(name = "cible_id")
    private UUID cibleId;

    // Relations optionnelles (pour faciliter les requêtes)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cible_id", insertable = false, updatable = false)
    private Livre livre;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cible_id", insertable = false, updatable = false)
    private Categorie categorie;

    @Column(nullable = false, name = "date_debut")
    private LocalDate dateDebut;

    @Column(nullable = false, name = "date_fin")
    private LocalDate dateFin;

    @Column(nullable = false)
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Méthode utilitaire pour vérifier si la réduction est valide
    public boolean estValide() {
        LocalDate aujourdhui = LocalDate.now();
        return actif && 
               !aujourdhui.isBefore(dateDebut) && 
               !aujourdhui.isAfter(dateFin);
    }

    // Méthode pour calculer le montant de réduction
    public BigDecimal calculerMontantReduction(BigDecimal prixOriginal) {
        if (!estValide()) {
            return BigDecimal.ZERO;
        }

        if (type == TypeReduction.POURCENTAGE) {
            return prixOriginal.multiply(valeur).divide(new BigDecimal("100"));
        } else {
            return valeur;
        }
    }
}