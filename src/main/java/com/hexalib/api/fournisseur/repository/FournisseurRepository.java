package com.hexalib.api.fournisseur.repository;

import com.hexalib.api.fournisseur.model.Fournisseur;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FournisseurRepository extends JpaRepository<Fournisseur, String> {
    
    Optional<Fournisseur> findByNom(String nom);
    
    Optional<Fournisseur> findByEmail(String email);
    
    boolean existsByNom(String nom);
    
    boolean existsByEmail(String email);
    
    boolean existsByNomAndIdNot(String nom, String id);
    
    boolean existsByEmailAndIdNot(String email, String id);
    
    /**
     * Recherche par nom, contact, téléphone ou email
     */
    @Query("SELECT f FROM Fournisseur f WHERE " +
           "LOWER(f.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.contact) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.telephone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.email) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Fournisseur> searchFournisseurs(@Param("search") String search, Pageable pageable);
    
    /**
     * Filtrer par statut
     */
    Page<Fournisseur> findByStatut(Fournisseur.Statut statut, Pageable pageable);
    
    /**
     * Recherche avec filtre statut
     */
    @Query("SELECT f FROM Fournisseur f WHERE " +
           "(:search IS NULL OR " +
           "LOWER(f.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.contact) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.telephone) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(f.email) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:statut IS NULL OR f.statut = :statut)")
    Page<Fournisseur> searchWithFilters(
        @Param("search") String search,
        @Param("statut") Fournisseur.Statut statut,
        Pageable pageable
    );
}