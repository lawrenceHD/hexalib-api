package com.hexalib.api.stock.model;

import com.hexalib.api.auth.model.User;
import com.hexalib.api.livre.model.Livre;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "mouvements_stock", indexes = {
    @Index(name = "idx_livre", columnList = "livre_id"),
    @Index(name = "idx_date", columnList = "date_mouvement")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MouvementStock {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(columnDefinition = "VARCHAR(36)")
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "livre_id", nullable = false)
    private Livre livre;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_mouvement", nullable = false, length = 20)
    private TypeMouvement typeMouvement;

    @Column(nullable = false)
    private Integer quantite;

    @Column(name = "stock_avant", nullable = false)
    private Integer stockAvant;

    @Column(name = "stock_apres", nullable = false)
    private Integer stockApres;

    @Column(length = 255)
    private String motif;

    @Column(length = 50)
    private String reference;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "date_mouvement", nullable = false)
    private LocalDateTime dateMouvement;
}