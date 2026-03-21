package com.hexalib.api.comptabilite.repository;

import com.hexalib.api.comptabilite.model.CategorieDepense;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CategorieDepenseRepository extends JpaRepository<CategorieDepense, String> {

    boolean existsByNom(String nom);
    boolean existsByNomAndIdNot(String nom, String id);
    List<CategorieDepense> findByStatut(CategorieDepense.Statut statut);
}