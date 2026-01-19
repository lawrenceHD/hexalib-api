package com.hexalib.api.vente.repository;

import com.hexalib.api.vente.model.LigneVente;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface LigneVenteRepository extends JpaRepository<LigneVente, UUID> {

    @Query("SELECT lv FROM LigneVente lv " +
           "WHERE lv.vente.statut = 'VALIDEE' " +
           "AND lv.vente.dateVente BETWEEN :debut AND :fin " +
           "GROUP BY lv.livre.id " +
           "ORDER BY SUM(lv.quantite) DESC")
    List<LigneVente> findTopLivresVendus(
        @Param("debut") LocalDateTime debut,
        @Param("fin") LocalDateTime fin
    );
}