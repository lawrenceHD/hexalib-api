package com.hexalib.api.stock.repository;

import com.hexalib.api.stock.model.MouvementStock;
import com.hexalib.api.stock.model.TypeMouvement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface MouvementStockRepository extends JpaRepository<MouvementStock, UUID> {

    // Mouvements par livre
    Page<MouvementStock> findByLivreIdOrderByDateMouvementDesc(String livreId, Pageable pageable);

    // Mouvements par type
    Page<MouvementStock> findByTypeMouvementOrderByDateMouvementDesc(TypeMouvement type, Pageable pageable);

    // Mouvements par utilisateur
    Page<MouvementStock> findByUserIdOrderByDateMouvementDesc(String userId, Pageable pageable);

    // Mouvements par période
    @Query("SELECT m FROM MouvementStock m WHERE m.dateMouvement BETWEEN :debut AND :fin " +
           "ORDER BY m.dateMouvement DESC")
    Page<MouvementStock> findByPeriode(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin,
        Pageable pageable
    );

    // Mouvements avec filtres combinés
    @Query("SELECT m FROM MouvementStock m WHERE " +
           "(:livreId IS NULL OR m.livre.id = :livreId) AND " +
           "(:type IS NULL OR m.typeMouvement = :type) AND " +
           "(:userId IS NULL OR m.user.id = :userId) AND " +
           "(:debut IS NULL OR m.dateMouvement >= :debut) AND " +
           "(:fin IS NULL OR m.dateMouvement <= :fin) " +
           "ORDER BY m.dateMouvement DESC")
    Page<MouvementStock> findWithFilters(
        @Param("livreId") String livreId,
        @Param("type") TypeMouvement type,
        @Param("userId") String userId,
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin,
        Pageable pageable
    );

    // Historique complet d'un livre
    List<MouvementStock> findByLivreIdOrderByDateMouvementDesc(String livreId);

    // Dernier mouvement d'un livre
    @Query("SELECT m FROM MouvementStock m WHERE m.livre.id = :livreId " +
           "ORDER BY m.dateMouvement DESC")
    List<MouvementStock> findLastByLivre(@Param("livreId") String livreId);
}