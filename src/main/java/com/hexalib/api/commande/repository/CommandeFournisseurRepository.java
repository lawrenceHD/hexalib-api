package com.hexalib.api.commande.repository;

import com.hexalib.api.commande.model.CommandeFournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface CommandeFournisseurRepository extends JpaRepository<CommandeFournisseur, String> {
    
    Optional<CommandeFournisseur> findByNumeroCommande(String numeroCommande);
    
    boolean existsByNumeroCommande(String numeroCommande);
    
    /**
     * Recherche par numéro de commande ou fournisseur
     */
    @Query("SELECT c FROM CommandeFournisseur c LEFT JOIN FETCH c.fournisseur WHERE " +
           "LOWER(c.numeroCommande) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.fournisseur.nom) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<CommandeFournisseur> searchCommandes(@Param("search") String search, Pageable pageable);
    
    /**
     * Filtrer par fournisseur
     */
    Page<CommandeFournisseur> findByFournisseurId(String fournisseurId, Pageable pageable);
    
    /**
     * Filtrer par statut
     */
    Page<CommandeFournisseur> findByStatut(CommandeFournisseur.Statut statut, Pageable pageable);
    
    /**
     * Recherche avec filtres combinés
     */
    @Query("SELECT c FROM CommandeFournisseur c LEFT JOIN FETCH c.fournisseur f WHERE " +
           "(:search IS NULL OR " +
           "LOWER(c.numeroCommande) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.nom) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:fournisseurId IS NULL OR c.fournisseur.id = :fournisseurId) AND " +
           "(:statut IS NULL OR c.statut = :statut) AND " +
           "(:dateDebut IS NULL OR c.dateCommande >= :dateDebut) AND " +
           "(:dateFin IS NULL OR c.dateCommande <= :dateFin)")
    Page<CommandeFournisseur> searchWithFilters(
        @Param("search") String search,
        @Param("fournisseurId") String fournisseurId,
        @Param("statut") CommandeFournisseur.Statut statut,
        @Param("dateDebut") LocalDate dateDebut,
        @Param("dateFin") LocalDate dateFin,
        Pageable pageable
    );
    
    /**
     * Compter les commandes par fournisseur
     */
    long countByFournisseurId(String fournisseurId);
}