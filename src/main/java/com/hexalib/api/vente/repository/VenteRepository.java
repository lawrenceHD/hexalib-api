package com.hexalib.api.vente.repository;

import com.hexalib.api.vente.model.StatutVente;
import com.hexalib.api.vente.model.Vente;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface VenteRepository extends JpaRepository<Vente, String> {

    Optional<Vente> findByNumeroFacture(String numeroFacture);

    Page<Vente> findByVendeurIdOrderByDateVenteDesc(String vendeurId, Pageable pageable);

    Page<Vente> findByStatutOrderByDateVenteDesc(StatutVente statut, Pageable pageable);

    @Query("SELECT v FROM Vente v WHERE v.numeroFacture LIKE %:search% " +
           "OR v.vendeur.nomComplet LIKE %:search% " +
           "ORDER BY v.dateVente DESC")
    Page<Vente> search(@Param("search") String search, Pageable pageable);

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND DATE(v.dateVente) = :date")
    long countByDate(@Param("date") LocalDate date);

    @Query("SELECT v FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.dateVente BETWEEN :debut AND :fin " +
           "ORDER BY v.dateVente DESC")
    List<Vente> findByPeriode(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );

    @Query("SELECT SUM(v.montantTTC) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND DATE(v.dateVente) = :date")
    BigDecimal sumMontantByDate(@Param("date") LocalDate date);

    @Query("SELECT SUM(v.montantTTC) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.vendeur.id = :vendeurId " +
           "AND DATE(v.dateVente) = :date")
    BigDecimal sumMontantByVendeurAndDate(
        @Param("vendeurId") String vendeurId,
        @Param("date") LocalDate date
    );

    @Query("SELECT COUNT(v) FROM Vente v WHERE v.statut = 'VALIDEE' " +
           "AND v.vendeur.id = :vendeurId " +
           "AND DATE(v.dateVente) = :date")
    long countByVendeurAndDate(
        @Param("vendeurId") String vendeurId,
        @Param("date") LocalDate date
    );

    // Compter les ventes d'un vendeur entre deux dates
@Query("SELECT COUNT(v) FROM Vente v WHERE v.statut = 'VALIDEE' " +
       "AND v.vendeur.id = :vendeurId " +
       "AND v.dateVente BETWEEN :debut AND :fin")
long countByVendeurBetween(
    @Param("vendeurId") String vendeurId,
    @Param("debut") LocalDateTime debut,
    @Param("fin") LocalDateTime fin
);

// Somme CA d'un vendeur entre deux dates
@Query("SELECT SUM(v.montantTTC) FROM Vente v WHERE v.statut = 'VALIDEE' " +
       "AND v.vendeur.id = :vendeurId " +
       "AND v.dateVente BETWEEN :debut AND :fin")
BigDecimal sumMontantByVendeurBetween(
    @Param("vendeurId") String vendeurId,
    @Param("debut") LocalDateTime debut,
    @Param("fin") LocalDateTime fin
);
}