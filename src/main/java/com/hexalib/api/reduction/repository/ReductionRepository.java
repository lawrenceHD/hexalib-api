package com.hexalib.api.reduction.repository;

import com.hexalib.api.reduction.model.CibleReduction;
import com.hexalib.api.reduction.model.Reduction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ReductionRepository extends JpaRepository<Reduction, String> {

    Page<Reduction> findByIntituleContainingIgnoreCase(String intitule, Pageable pageable);

    Page<Reduction> findByActifTrue(Pageable pageable);

    @Query("SELECT r FROM Reduction r WHERE r.dateFin < :today")
    Page<Reduction> findExpired(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT r FROM Reduction r WHERE r.actif = true " +
           "AND r.dateDebut <= :today AND r.dateFin >= :today")
    List<Reduction> findValidReductions(@Param("today") LocalDate today);

    /**
     * Retourne TOUTES les réductions applicables à un livre à une date donnée,
     * triées par priorité (LIVRE > CATEGORIE > GLOBALE) puis par valeur décroissante
     */
    @Query("""
        SELECT r FROM Reduction r 
        WHERE r.actif = true 
          AND r.dateDebut <= :today 
          AND r.dateFin >= :today 
          AND (
            (r.cible = 'LIVRE' AND r.cibleId = :livreId)
            OR (r.cible = 'CATEGORIE' AND r.cibleId = :categorieId)
            OR (r.cible = 'GLOBALE')
          )
        ORDER BY 
          CASE r.cible 
            WHEN 'LIVRE' THEN 1 
            WHEN 'CATEGORIE' THEN 2 
            WHEN 'GLOBALE' THEN 3 
          END ASC,
          r.valeur DESC
        """)
    List<Reduction> findApplicableReductionsOrdered(
        @Param("livreId") String livreId,
        @Param("categorieId") String categorieId,
        @Param("today") LocalDate today
    );

    /**
     * Retourne la MEILLEURE réduction applicable (priorité + valeur la plus élevée).
     * Ne plante plus si plusieurs résultats.
     */
    default Optional<Reduction> findBestApplicableReductionForLivre(
        String livreId,
        String categorieId,
        LocalDate today
    ) {
        List<Reduction> ordered = findApplicableReductionsOrdered(livreId, categorieId, today);
        return ordered.isEmpty() ? Optional.empty() : Optional.of(ordered.get(0));
    }

    Page<Reduction> findByCible(CibleReduction cible, Pageable pageable);

    List<Reduction> findByCibleAndCibleId(CibleReduction cible, String cibleId);

    long countByActifTrue();
}