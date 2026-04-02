package com.hexalib.api.livre.service;

import com.hexalib.api.categorie.model.Categorie;
import com.hexalib.api.categorie.repository.CategorieRepository;
import com.hexalib.api.common.util.CodeGenerator;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class LivreImportLineService {

    private final CategorieRepository categorieRepository;
    private final LivreRepository     livreRepository;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public boolean traiterLigne(ImportJobStore.LigneParsee ligne,
                                Map<String, String> cacheCategories) {

        // 1. Obtenir l'ID de catégorie (créer si nécessaire)
        String categorieId = obtenirOuCreerCategorieId(ligne.categorie(), cacheCategories);

        // 2. Recharger l'entité dans la session courante (évite detached entity)
        Categorie categorie = categorieRepository.findById(categorieId)
                .orElseThrow(() -> new RuntimeException(
                        "Catégorie introuvable : " + categorieId));

        // 3. Vérifier doublon
        String auteur = ligne.auteur() != null ? ligne.auteur() : "";
        boolean existe = livreRepository
                .existsByTitreAndAuteurAndCategorieId(ligne.titre(), auteur, categorieId);
        if (existe) return false;

        // 4. Créer le livre
        Livre livre = new Livre();
        livre.setTitre(ligne.titre());
        livre.setAuteur(ligne.auteur());
        livre.setMaisonEdition(ligne.maisonEdition());
        livre.setPrixVente(ligne.prixVente() != null ? ligne.prixVente() : BigDecimal.ZERO);
        livre.setDescription(ligne.description());
        livre.setCategorie(categorie);
        livre.setQuantiteStock(0);
        livre.setSeuilMinimal(5);
        livre.setLangue("Français");
        livre.setStatut(ligne.inactif() ? Livre.Statut.INACTIF : Livre.Statut.ACTIF);
        livre.setCode(genererCodeLivre(categorie.getCode()));

        livreRepository.save(livre);
        return true;
    }

    private String obtenirOuCreerCategorieId(String nomCategorie,
                                              Map<String, String> cache) {
        if (nomCategorie == null || nomCategorie.isBlank()) {
            nomCategorie = "Non classé";
        }
        final String nom = nomCategorie;

        return cache.computeIfAbsent(nom.toLowerCase(), k ->
            categorieRepository.findByNomIgnoreCase(nom)
                    .map(Categorie::getId)
                    .orElseGet(() -> {
                        log.info("Création catégorie : {}", nom);
                        Categorie cat = new Categorie();
                        cat.setNom(nom);
                        cat.setStatut(Categorie.Statut.ACTIF);
                        cat.setCode(genererCodeCategorieUnique(nom));
                        return categorieRepository.save(cat).getId();
                    })
        );
    }

    private String genererCodeCategorieUnique(String nom) {
        String code;
        int attempts = 0;
        do {
            code = CodeGenerator.generateCategorieCode(nom);
            attempts++;
            if (attempts > 100) {
                throw new RuntimeException(
                        "Impossible de générer un code unique pour : " + nom);
            }
        } while (categorieRepository.existsByCode(code));
        return code;
    }

    private String genererCodeLivre(String codeCategorie) {
        int suivant = livreRepository.countByCategorieCode(codeCategorie) + 1;
        return String.format("%s-%03d", codeCategorie, suivant);
    }
}