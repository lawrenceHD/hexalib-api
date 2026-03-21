package com.hexalib.api.auth.repository;

import com.hexalib.api.auth.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, String> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    // Recherche + filtres combinés
    @Query("""
        SELECT u FROM User u
        WHERE (:search IS NULL OR :search = ''
               OR LOWER(u.nomComplet) LIKE LOWER(CONCAT('%', :search, '%'))
               OR LOWER(u.email)     LIKE LOWER(CONCAT('%', :search, '%')))
          AND (:role   IS NULL OR u.role   = :role)
          AND (:statut IS NULL OR u.statut = :statut)
        """)
    Page<User> findWithFilters(
            @Param("search") String search,
            @Param("role")   User.Role role,
            @Param("statut") User.Statut statut,
            Pageable pageable
    );

    // Vérifier si un utilisateur a des ventes (avant suppression)
    @Query("SELECT COUNT(v) > 0 FROM Vente v WHERE v.vendeur.id = :userId")
    boolean hasVentes(@Param("userId") String userId);
}