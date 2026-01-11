package com.hexalib.api.commande.repository;

import com.hexalib.api.commande.model.LigneCommandeFournisseur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LigneCommandeFournisseurRepository extends JpaRepository<LigneCommandeFournisseur, String> {
    
    List<LigneCommandeFournisseur> findByCommandeId(String commandeId);
    
    void deleteByCommandeId(String commandeId);
}