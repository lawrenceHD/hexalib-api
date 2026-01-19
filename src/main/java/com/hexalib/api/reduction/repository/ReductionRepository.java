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
public interface ReductionRepository extends JpaRepository<Reduction, UUID> {

    Page<Reduction> findByIntituleContainingIgnoreCase(String intitule, Pageable pageable);

    Page<Reduction> findByActifTrue(Pageable pageable);

    @Query("SELECT r FROM Reduction r WHERE r.dateFin < :today")
    Page<Reduction> findExpired(@Param("today") LocalDate today, Pageable pageable);

    @Query("SELECT r FROM Reduction r WHERE r.actif = true " +
           "AND r.dateDebut <= :today AND r.dateFin >= :today")
    List<Reduction> findValidReductions(@Param("today") LocalDate today);

    @Query("SELECT r FROM Reduction r WHERE r.actif = true " +
           "AND r.dateDebut <= :today AND r.dateFin >= :today " +
           "AND ((r.cible = 'GLOBALE') " +
           "OR (r.cible = 'LIVRE' AND r.cibleId = :livreId) " +
           "OR (r.cible = 'CATEGORIE' AND r.cibleId = :categorieId))")
    List<Reduction> findApplicableReductions(
        @Param("livreId") String livreId,
        @Param("categorieId") String categorieId,
        @Param("today") LocalDate today
    );

    @Query("SELECT r FROM Reduction r WHERE r.actif = true " +
           "AND r.dateDebut <= :today AND r.dateFin >= :today " +
           "AND ((r.cible = 'LIVRE' AND r.cibleId = :livreId) " +
           "OR (r.cible = 'CATEGORIE' AND r.cibleId = :categorieId) " +
           "OR (r.cible = 'GLOBALE')) " +
           "ORDER BY CASE r.cible " +
           "WHEN 'LIVRE' THEN 1 " +
           "WHEN 'CATEGORIE' THEN 2 " +
           "WHEN 'GLOBALE' THEN 3 END")
    Optional<Reduction> findBestReductionForLivre(
        @Param("livreId") String livreId,
        @Param("categorieId") String categorieId,
        @Param("today") LocalDate today
    );

    Page<Reduction> findByCible(CibleReduction cible, Pageable pageable);

    List<Reduction> findByCibleAndCibleId(CibleReduction cible, String cibleId);

    long countByActifTrue();
}