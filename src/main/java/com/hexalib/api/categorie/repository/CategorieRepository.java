package com.hexalib.api.categorie.repository;

import com.hexalib.api.categorie.model.Categorie;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CategorieRepository extends JpaRepository<Categorie, String> {
    
    Optional<Categorie> findByNom(String nom);
    
    Optional<Categorie> findByCode(String code);
    
    boolean existsByNom(String nom);
    
    boolean existsByCode(String code);
    
    boolean existsByNomAndIdNot(String nom, String id);
    
    @Query("SELECT c FROM Categorie c WHERE " +
           "LOWER(c.nom) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(c.code) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Categorie> searchCategories(@Param("search") String search, Pageable pageable);
    
    Page<Categorie> findByStatut(Categorie.Statut statut, Pageable pageable);
}