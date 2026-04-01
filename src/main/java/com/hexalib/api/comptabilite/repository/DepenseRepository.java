package com.hexalib.api.comptabilite.repository;

import com.hexalib.api.comptabilite.model.Depense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface DepenseRepository extends JpaRepository<Depense, String> {

    @Query("""
        SELECT d FROM Depense d
        WHERE (:categorieId IS NULL OR d.categorie.id = :categorieId)
          AND (:debut IS NULL OR d.dateDepense >= :debut)
          AND (:fin IS NULL OR d.dateDepense <= :fin)
        ORDER BY d.dateDepense DESC
        """)
    Page<Depense> findWithFilters(
        @Param("categorieId") String categorieId,
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin,
        Pageable pageable
    );

    @Query("""
        SELECT d FROM Depense d
        WHERE (:debut IS NULL OR d.dateDepense >= :debut)
          AND (:fin IS NULL OR d.dateDepense <= :fin)
        ORDER BY d.dateDepense ASC
        """)
    List<Depense> findByPeriode(
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin
    );

    @Query("""
        SELECT COALESCE(SUM(d.montant), 0)
        FROM Depense d
        WHERE d.dateDepense BETWEEN :debut AND :fin
        """)
    BigDecimal sumMontantByPeriode(
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin
    );

    // ✅ 3 colonnes : nom, montant, count — utilisé par buildDepensesParCategorie
    @Query("""
        SELECT d.categorie.nom, SUM(d.montant), COUNT(d)
        FROM Depense d
        WHERE d.dateDepense BETWEEN :debut AND :fin
        GROUP BY d.categorie.id, d.categorie.nom
        ORDER BY SUM(d.montant) DESC
        """)
    List<Object[]> sumMontantByCategorieAndPeriode(
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin
    );

    // ✅ CORRIGÉ : aussi 3 colonnes (nom, montant, count) pour être cohérent
    //    avec buildDepensesParCategorie qui lit row[0], row[1], row[2]
    @Query("""
        SELECT d.categorie.nom, SUM(d.montant), COUNT(d)
        FROM Depense d
        WHERE d.dateDepense BETWEEN :debut AND :fin
        GROUP BY d.categorie.id, d.categorie.nom
        ORDER BY SUM(d.montant) DESC
        """)
    List<Object[]> findTop5Categories(
        @Param("debut") LocalDate debut,
        @Param("fin") LocalDate fin,
        Pageable pageable
    );
}