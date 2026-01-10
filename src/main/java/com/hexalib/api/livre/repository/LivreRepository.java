package com.hexalib.api.livre.repository;

import com.hexalib.api.livre.model.Livre;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface LivreRepository extends JpaRepository<Livre, String> {
    
    Optional<Livre> findByCode(String code);
    
    Optional<Livre> findByIsbn(String isbn);
    
    boolean existsByCode(String code);
    
    boolean existsByIsbn(String isbn);
    
    boolean existsByTitreAndAuteur(String titre, String auteur);
    
    boolean existsByTitreAndAuteurAndIdNot(String titre, String auteur, String id);
    
    /**
     * Recherche avancée multi-critères
     */
    @Query("SELECT l FROM Livre l WHERE " +
           "LOWER(l.titre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.auteur) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :search, '%'))")
    Page<Livre> searchLivres(@Param("search") String search, Pageable pageable);
    
    /**
     * Filtre par catégorie
     */
    Page<Livre> findByCategorieId(String categorieId, Pageable pageable);
    
    /**
     * Filtre par statut
     */
    Page<Livre> findByStatut(Livre.Statut statut, Pageable pageable);
    
    /**
     * Filtre par langue
     */
    Page<Livre> findByLangue(String langue, Pageable pageable);
    
    /**
     * Livres en stock critique (quantité <= seuil)
     */
    @Query("SELECT l FROM Livre l WHERE l.quantiteStock <= l.seuilMinimal AND l.statut = 'ACTIF'")
    List<Livre> findLivresStockCritique();
    
    /**
     * Livres en rupture de stock
     */
    @Query("SELECT l FROM Livre l WHERE l.quantiteStock = 0 AND l.statut = 'ACTIF'")
    List<Livre> findLivresEnRupture();
    
    /**
     * Recherche avec filtres combinés
     */
    @Query("SELECT l FROM Livre l WHERE " +
           "(:search IS NULL OR " +
           "LOWER(l.titre) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.auteur) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.code) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
           "LOWER(l.isbn) LIKE LOWER(CONCAT('%', :search, '%'))) AND " +
           "(:categorieId IS NULL OR l.categorie.id = :categorieId) AND " +
           "(:statut IS NULL OR l.statut = :statut) AND " +
           "(:langue IS NULL OR l.langue = :langue)")
    Page<Livre> searchWithFilters(
        @Param("search") String search,
        @Param("categorieId") String categorieId,
        @Param("statut") Livre.Statut statut,
        @Param("langue") String langue,
        Pageable pageable
    );
    
    /**
     * Compter les livres par catégorie
     */
    long countByCategorieId(String categorieId);
    
    /**
     * Lister toutes les langues disponibles (distinct)
     */
    @Query("SELECT DISTINCT l.langue FROM Livre l ORDER BY l.langue")
    List<String> findAllLangues();
}