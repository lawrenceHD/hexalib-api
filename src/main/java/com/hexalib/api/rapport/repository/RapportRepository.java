package com.hexalib.api.rapport.repository;

import com.hexalib.api.rapport.dto.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RapportRepository extends JpaRepository<Object, String> {

    // ==================== STATISTIQUES GLOBALES ====================
    
    /**
     * Nombre de ventes pour une période donnée
     */
    @Query("SELECT COUNT(v) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    long countVentesByPeriode(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Chiffre d'affaires pour une période donnée
     */
    @Query("SELECT COALESCE(SUM(v.montantTTC), 0) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    BigDecimal sumCAByPeriode(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Montant total des réductions pour une période
     */
    @Query("SELECT COALESCE(SUM(v.montantReductions), 0) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    BigDecimal sumReductionsByPeriode(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Nombre total de livres vendus (quantité)
     */
    @Query("SELECT COALESCE(SUM(lv.quantite), 0) FROM LigneVente lv " +
           "JOIN lv.vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    long sumQuantiteLivresVendus(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== ÉVOLUTION CA ====================
    
    /**
     * Évolution du CA jour par jour (pour graphique)
     */
    @Query("SELECT new com.hexalib.api.rapport.dto.EvolutionCADTO(" +
           "CAST(v.dateVente AS LocalDate), " +
           "COALESCE(SUM(v.montantTTC), 0), " +
           "COUNT(v)) " +
           "FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY CAST(v.dateVente AS LocalDate) " +
           "ORDER BY CAST(v.dateVente AS LocalDate)")
    List<EvolutionCADTO> getEvolutionCA(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== TOP LIVRES ====================
    
    /**
     * Top N livres les plus vendus (par quantité)
     */
    @Query("SELECT new com.hexalib.api.rapport.dto.TopLivreDTO(" +
           "l.id, l.code, l.titre, l.auteur, c.nom, " +
           "SUM(lv.quantite), " +
           "SUM(lv.sousTotal), " +
           "COUNT(DISTINCT v.id), " +
           "0) " +
           "FROM LigneVente lv " +
           "JOIN lv.livre l " +
           "JOIN l.categorie c " +
           "JOIN lv.vente v " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY l.id, l.code, l.titre, l.auteur, c.nom " +
           "ORDER BY SUM(lv.quantite) DESC")
    List<TopLivreDTO> getTopLivres(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== TOP CATÉGORIES ====================
    
    /**
     * Top N catégories les plus vendues
     */
    @Query("SELECT new com.hexalib.api.rapport.dto.TopCategorieDTO(" +
           "c.id, c.nom, c.code, " +
           "SUM(lv.quantite), " +
           "SUM(lv.sousTotal), " +
           "COUNT(DISTINCT v.id), " +
           "0) " +
           "FROM LigneVente lv " +
           "JOIN lv.livre l " +
           "JOIN l.categorie c " +
           "JOIN lv.vente v " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY c.id, c.nom, c.code " +
           "ORDER BY SUM(lv.quantite) DESC")
    List<TopCategorieDTO> getTopCategories(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== PERFORMANCE VENDEURS ====================
    
    /**
     * Performance de tous les vendeurs
     */
    @Query("SELECT new com.hexalib.api.rapport.dto.PerformanceVendeurDTO(" +
           "u.id, u.nomComplet, " +
           "COUNT(v.id), " +
           "COALESCE(SUM(v.montantTTC), 0), " +
           "COALESCE(SUM(lv.quantite), 0), " +
           "COALESCE(AVG(v.montantTTC), 0), " +
           "0) " +
           "FROM Vente v " +
           "JOIN v.vendeur u " +
           "LEFT JOIN v.lignes lv " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY u.id, u.nomComplet " +
           "ORDER BY SUM(v.montantTTC) DESC")
    List<PerformanceVendeurDTO> getPerformanceVendeurs(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== STOCK CRITIQUE ====================
    
    /**
     * Livres en stock critique ou en rupture
     */
    @Query("SELECT new com.hexalib.api.rapport.dto.LivreStockCritiqueDTO(" +
           "l.id, l.code, l.titre, l.auteur, c.nom, " +
           "l.quantiteStock, l.seuilMinimal, " +
           "CASE WHEN l.quantiteStock = 0 THEN 'RUPTURE' ELSE 'CRITIQUE' END) " +
           "FROM Livre l " +
           "JOIN l.categorie c " +
           "WHERE l.statut = 'ACTIF' " +
           "AND l.quantiteStock <= l.seuilMinimal " +
           "ORDER BY l.quantiteStock ASC")
    List<LivreStockCritiqueDTO> getLivresStockCritique();

    /**
     * Nombre de livres en stock critique
     */
    @Query("SELECT COUNT(l) FROM Livre l WHERE l.statut = 'ACTIF' " +
           "AND l.quantiteStock <= l.seuilMinimal")
    long countLivresStockCritique();

    // ==================== ANALYSE RÉDUCTIONS ====================
    
    /**
     * Nombre de ventes avec réduction
     */
    @Query("SELECT COUNT(DISTINCT v.id) FROM Vente v " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.montantReductions > 0 " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    long countVentesAvecReduction(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Réduction moyenne
     */
    @Query("SELECT COALESCE(AVG(v.montantReductions), 0) FROM Vente v " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.montantReductions > 0 " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    BigDecimal getReductionMoyenne(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
    
    /**
     * Réduction maximale
     */
    @Query("SELECT COALESCE(MAX(v.montantReductions), 0) FROM Vente v " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    BigDecimal getReductionMaximale(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== ROTATION STOCK ====================
    
    /**
     * Rotation du stock par catégorie
     */
    @Query("SELECT new com.hexalib.api.rapport.dto.RotationStockDTO(" +
           "c.nom, " +
           "COALESCE(SUM(lv.quantite), 0), " +
           "COALESCE(SUM(l.quantiteStock), 0), " +
           "0) " +
           "FROM Categorie c " +
           "LEFT JOIN Livre l ON l.categorie.id = c.id " +
           "LEFT JOIN LigneVente lv ON lv.livre.id = l.id " +
           "LEFT JOIN lv.vente v ON v.id = lv.vente.id AND v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "WHERE c.statut = 'ACTIF' " +
           "GROUP BY c.nom " +
           "ORDER BY SUM(lv.quantite) DESC")
    List<RotationStockDTO> getRotationStock(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    // ==================== MARGE BÉNÉFICIAIRE ====================
    
    /**
     * Calculer la marge bénéficiaire totale (si prix d'achat renseigné)
     */
    @Query("SELECT COALESCE(SUM((lv.prixUnitaire - l.prixAchat) * lv.quantite), 0) " +
           "FROM LigneVente lv " +
           "JOIN lv.livre l " +
           "JOIN lv.vente v " +
           "WHERE v.statut = 'VALIDEE' " +
           "AND l.prixAchat IS NOT NULL " +
           "AND v.dateVente BETWEEN :debut AND :fin")
    BigDecimal getMargeBeneficiaire(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
}