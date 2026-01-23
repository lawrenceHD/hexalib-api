package com.hexalib.api.vente.model;

import com.hexalib.api.auth.model.User;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ventes", indexes = {
    @Index(name = "idx_numero", columnList = "numero_facture"),
    @Index(name = "idx_date", columnList = "date_vente"),
    @Index(name = "idx_vendeur", columnList = "vendeur_id")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Vente {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(name = "numero_facture", nullable = false, unique = true, length = 30)
    private String numeroFacture;

    @Column(name = "date_vente", nullable = false)
    private LocalDateTime dateVente;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vendeur_id", nullable = false)
    private User vendeur;

    @Column(name = "montant_ht", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantHT;

    @Column(name = "montant_reductions", precision = 10, scale = 2)
    private BigDecimal montantReductions = BigDecimal.ZERO;

    @Column(name = "montant_ttc", nullable = false, precision = 10, scale = 2)
    private BigDecimal montantTTC;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutVente statut = StatutVente.VALIDEE;

    @Column(name = "motif_annulation", columnDefinition = "TEXT")
    private String motifAnnulation;

    @OneToMany(mappedBy = "vente", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LigneVente> lignes = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    // Méthodes utilitaires
    public void addLigne(LigneVente ligne) {
        lignes.add(ligne);
        ligne.setVente(this);
    }

    public void removeLigne(LigneVente ligne) {
        lignes.remove(ligne);
        ligne.setVente(null);
    }
}