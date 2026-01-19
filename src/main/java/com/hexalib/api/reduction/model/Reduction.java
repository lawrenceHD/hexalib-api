package com.hexalib.api.reduction.model;

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
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, length = 100)
    private String intitule;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TypeReduction type;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal valeur;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CibleReduction cible;

    @Column(name = "cible_id", columnDefinition = "VARCHAR(36)")
    private String cibleId;

    @Column(nullable = false, name = "date_debut")
    private LocalDate dateDebut;

    @Column(nullable = false, name = "date_fin")
    private LocalDate dateFin;

    @Column(nullable = false, columnDefinition = "TINYINT(1) DEFAULT 1")
    private Boolean actif = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean estValide() {
        LocalDate aujourdhui = LocalDate.now();
        return actif && 
               !aujourdhui.isBefore(dateDebut) && 
               !aujourdhui.isAfter(dateFin);
    }

    public BigDecimal calculerMontantReduction(BigDecimal prixOriginal) {
        if (!estValide()) {
            return BigDecimal.ZERO;
        }

        if (type == TypeReduction.POURCENTAGE) {
            return prixOriginal.multiply(valeur)
                    .divide(new BigDecimal("100"), 2, java.math.RoundingMode.HALF_UP);
        } else {
            return valeur;
        }
    }
}