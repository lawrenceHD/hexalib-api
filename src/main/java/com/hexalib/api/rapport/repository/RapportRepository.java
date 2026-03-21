package com.hexalib.api.rapport.repository;

import com.hexalib.api.rapport.dto.*;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public class RapportRepository {

    @PersistenceContext
    private EntityManager em;

    // ==================== STATISTIQUES GLOBALES ====================

    public long countVentesByPeriode(LocalDateTime debut, LocalDateTime fin) {
        return (long) em.createQuery(
                "SELECT COUNT(v) FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
    }

    public BigDecimal sumCAByPeriode(LocalDateTime debut, LocalDateTime fin) {
        BigDecimal result = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(v.montantTTC), 0) FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public BigDecimal sumReductionsByPeriode(LocalDateTime debut, LocalDateTime fin) {
        BigDecimal result = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(v.montantReductions), 0) FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public long sumQuantiteLivresVendus(LocalDateTime debut, LocalDateTime fin) {
        Object result = em.createQuery(
                "SELECT COALESCE(SUM(lv.quantite), 0) FROM LigneVente lv " +
                "JOIN lv.vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    // ==================== STATISTIQUES PAR VENDEUR ====================

    /**
     * Stats d'un vendeur spécifique sur une période
     */
    public long countVentesByVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        return (long) em.createQuery(
                "SELECT COUNT(v) FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.vendeur.id = :vendeurId " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("vendeurId", vendeurId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
    }

    public BigDecimal sumCAByVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        BigDecimal result = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(v.montantTTC), 0) FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.vendeur.id = :vendeurId " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("vendeurId", vendeurId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public BigDecimal sumReductionsByVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        BigDecimal result = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM(v.montantReductions), 0) FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.vendeur.id = :vendeurId " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("vendeurId", vendeurId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }

    public long sumQuantiteLivresVendusByVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        Object result = em.createQuery(
                "SELECT COALESCE(SUM(lv.quantite), 0) FROM LigneVente lv " +
                "JOIN lv.vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.vendeur.id = :vendeurId " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("vendeurId", vendeurId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? ((Number) result).longValue() : 0L;
    }

    /**
     * Top livres vendus par un vendeur spécifique
     */
    @SuppressWarnings("unchecked")
    public List<TopLivreDTO> getTopLivresByVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery(
                "SELECT new com.hexalib.api.rapport.dto.TopLivreDTO(" +
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
                "AND v.vendeur.id = :vendeurId " +
                "AND v.dateVente BETWEEN :debut AND :fin " +
                "GROUP BY l.id, l.code, l.titre, l.auteur, c.nom " +
                "ORDER BY SUM(lv.quantite) DESC")
                .setParameter("vendeurId", vendeurId)
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    // ==================== ÉVOLUTION CA ====================

    @SuppressWarnings("unchecked")
    public List<EvolutionCADTO> getEvolutionCA(LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery(
                "SELECT new com.hexalib.api.rapport.dto.EvolutionCADTO(" +
                "CAST(v.dateVente AS LocalDate), " +
                "COALESCE(SUM(v.montantTTC), 0), " +
                "COUNT(v)) " +
                "FROM Vente v WHERE v.statut = 'VALIDEE' " +
                "AND v.dateVente BETWEEN :debut AND :fin " +
                "GROUP BY CAST(v.dateVente AS LocalDate) " +
                "ORDER BY CAST(v.dateVente AS LocalDate)")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    // ==================== TOP LIVRES ====================

    @SuppressWarnings("unchecked")
    public List<TopLivreDTO> getTopLivres(LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery(
                "SELECT new com.hexalib.api.rapport.dto.TopLivreDTO(" +
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
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    // ==================== TOP CATÉGORIES ====================

    @SuppressWarnings("unchecked")
    public List<TopCategorieDTO> getTopCategories(LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery(
                "SELECT new com.hexalib.api.rapport.dto.TopCategorieDTO(" +
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
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    // ==================== PERFORMANCE VENDEURS ====================

    @SuppressWarnings("unchecked")
public List<PerformanceVendeurDTO> getPerformanceVendeurs(LocalDateTime debut, LocalDateTime fin) {
    return em.createQuery(
            "SELECT new com.hexalib.api.rapport.dto.PerformanceVendeurDTO(" +
            "u.id, u.nomComplet, " +
            "COUNT(v.id), " +
            "SUM(v.montantTTC), " +
            "SUM(lv.quantite)) " +
            "FROM Vente v " +
            "JOIN v.vendeur u " +
            "LEFT JOIN v.lignes lv " +
            "WHERE v.statut = 'VALIDEE' " +
            "AND v.dateVente BETWEEN :debut AND :fin " +
            "GROUP BY u.id, u.nomComplet " +
            "ORDER BY SUM(v.montantTTC) DESC")
            .setParameter("debut", debut)
            .setParameter("fin", fin)
            .getResultList();
}

    // ==================== STOCK CRITIQUE ====================

    @SuppressWarnings("unchecked")
    public List<LivreStockCritiqueDTO> getLivresStockCritique() {
        return em.createQuery(
                "SELECT new com.hexalib.api.rapport.dto.LivreStockCritiqueDTO(" +
                "l.id, l.code, l.titre, l.auteur, c.nom, " +
                "l.quantiteStock, l.seuilMinimal, " +
                "CASE WHEN l.quantiteStock = 0 THEN 'RUPTURE' ELSE 'CRITIQUE' END) " +
                "FROM Livre l " +
                "JOIN l.categorie c " +
                "WHERE l.statut = 'ACTIF' " +
                "AND l.quantiteStock <= l.seuilMinimal " +
                "ORDER BY l.quantiteStock ASC")
                .getResultList();
    }

    public long countLivresStockCritique() {
        return (long) em.createQuery(
                "SELECT COUNT(l) FROM Livre l WHERE l.statut = 'ACTIF' " +
                "AND l.quantiteStock <= l.seuilMinimal")
                .getSingleResult();
    }

    // ==================== ANALYSE RÉDUCTIONS ====================

    public long countVentesAvecReduction(LocalDateTime debut, LocalDateTime fin) {
        return (long) em.createQuery(
                "SELECT COUNT(DISTINCT v.id) FROM Vente v " +
                "WHERE v.statut = 'VALIDEE' " +
                "AND v.montantReductions > 0 " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
    }

    public BigDecimal getReductionMoyenne(LocalDateTime debut, LocalDateTime fin) {
        // AVG() retourne Double en JPQL/MySQL — ne pas caster en BigDecimal directement
        Object result = em.createQuery(
                "SELECT AVG(v.montantReductions) FROM Vente v " +
                "WHERE v.statut = 'VALIDEE' AND v.montantReductions > 0 " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        if (result == null) return BigDecimal.ZERO;
        return new BigDecimal(result.toString());
    }

    public BigDecimal getReductionMaximale(LocalDateTime debut, LocalDateTime fin) {
        // MAX() sur DECIMAL — retirer COALESCE(..., 0) qui peut forcer un type entier
        Object result = em.createQuery(
                "SELECT MAX(v.montantReductions) FROM Vente v " +
                "WHERE v.statut = 'VALIDEE' " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        if (result == null) return BigDecimal.ZERO;
        return new BigDecimal(result.toString());
    }

    // ==================== ROTATION STOCK ====================

    @SuppressWarnings("unchecked")
    public List<RotationStockDTO> getRotationStock(LocalDateTime debut, LocalDateTime fin) {
        return em.createQuery(
                "SELECT new com.hexalib.api.rapport.dto.RotationStockDTO(" +
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
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getResultList();
    }

    // ==================== MARGE BÉNÉFICIAIRE ====================

    public BigDecimal getMargeBeneficiaire(LocalDateTime debut, LocalDateTime fin) {
        BigDecimal result = (BigDecimal) em.createQuery(
                "SELECT COALESCE(SUM((lv.prixUnitaire - l.prixAchat) * lv.quantite), 0) " +
                "FROM LigneVente lv " +
                "JOIN lv.livre l " +
                "JOIN lv.vente v " +
                "WHERE v.statut = 'VALIDEE' " +
                "AND l.prixAchat IS NOT NULL " +
                "AND v.dateVente BETWEEN :debut AND :fin")
                .setParameter("debut", debut)
                .setParameter("fin", fin)
                .getSingleResult();
        return result != null ? result : BigDecimal.ZERO;
    }
}