package com.hexalib.api.comptabilite.service;

import com.hexalib.api.comptabilite.dto.*;
import com.hexalib.api.comptabilite.repository.DepenseRepository;
import com.hexalib.api.livre.model.Livre;
import com.hexalib.api.livre.repository.LivreRepository;
import com.hexalib.api.vente.model.Vente;
import com.hexalib.api.vente.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class ComptabiliteService {

    private final VenteRepository    venteRepository;
    private final DepenseRepository  depenseRepository;
    private final LivreRepository    livreRepository;

    // ══════════════════════════════════════════════════
    // DASHBOARD
    // ══════════════════════════════════════════════════

    public DashboardComptaDTO getDashboard(LocalDate debut, LocalDate fin) {
        log.info("Dashboard compta du {} au {}", debut, fin);

        // Entrées : ventes validées
        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT   = fin.atTime(LocalTime.MAX);

       // REMPLACER dans getDashboard() :
        BigDecimal totalEntrees = venteRepository.sumMontantByPeriode(debutDT, finDT);

        BigDecimal totalReductions = getSumReductionsByPeriode(debutDT, finDT);
        long       nombreVentes    = venteRepository.countByPeriode(debutDT, finDT); // approx — à améliorer

        // Sorties : dépenses
        BigDecimal totalSorties   = depenseRepository.sumMontantByPeriode(debut, fin);
        long       nbDepenses     = depenseRepository.findWithFilters(null, debut, fin,
                                        PageRequest.of(0, 1)).getTotalElements();

        // Solde
        BigDecimal solde = (totalEntrees != null ? totalEntrees : BigDecimal.ZERO)
                .subtract(totalSorties != null ? totalSorties : BigDecimal.ZERO);

        // Dépenses par catégorie
        List<Object[]> rawCats = depenseRepository.sumMontantByCategorieAndPeriode(debut, fin);
        List<DashboardComptaDTO.DepenseParCategorie> depensesParCategorie =
                buildDepensesParCategorie(rawCats, totalSorties);

        // Top 5
        List<Object[]> top5Raw = depenseRepository.findTop5Categories(
                debut, fin, PageRequest.of(0, 5));
        List<DashboardComptaDTO.DepenseParCategorie> top5 =
                buildDepensesParCategorie(top5Raw, totalSorties);

        return DashboardComptaDTO.builder()
                .periode(debut + " → " + fin)
                .totalEntrees(totalEntrees != null ? totalEntrees : BigDecimal.ZERO)
                .nombreVentes(nombreVentes)
                .totalSorties(totalSorties != null ? totalSorties : BigDecimal.ZERO)
                .nombreDepenses(nbDepenses)
                .soldeNet(solde)
                .totalReductions(totalReductions)
                .depensesParCategorie(depensesParCategorie)
                .top5Categories(top5)
                .build();
    }

    // ══════════════════════════════════════════════════
    // RAPPORT VENTES
    // ══════════════════════════════════════════════════

    public RapportVentesDTO getRapportVentes(LocalDate debut, LocalDate fin, String typeRapport) {
        log.info("Rapport ventes {} du {} au {}", typeRapport, debut, fin);

        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT   = fin.atTime(LocalTime.MAX);

        List<Vente> toutesVentes = venteRepository.findByPeriode(debutDT, finDT);

        List<Vente> ventesAvec  = toutesVentes.stream()
                .filter(v -> v.getMontantReductions().compareTo(BigDecimal.ZERO) > 0)
                .collect(Collectors.toList());

        List<Vente> ventesSans  = toutesVentes.stream()
                .filter(v -> v.getMontantReductions().compareTo(BigDecimal.ZERO) == 0)
                .collect(Collectors.toList());

        // Sélectionner les ventes à afficher selon le type
        List<Vente> ventesAffichees = switch (typeRapport) {
            case "AVEC_REDUCTION"  -> ventesAvec;
            case "SANS_REDUCTION"  -> ventesSans;
            default                -> toutesVentes; // COMBINE
        };

        BigDecimal caTotal         = sum(toutesVentes, Vente::getMontantTTC);
        BigDecimal totalReductions = sum(toutesVentes, Vente::getMontantReductions);
        BigDecimal caAvec          = sum(ventesAvec, Vente::getMontantTTC);
        BigDecimal caSans          = sum(ventesSans, Vente::getMontantTTC);

        List<RapportVentesDTO.LigneVenteRapport> lignes = ventesAffichees.stream()
                .map(v -> RapportVentesDTO.LigneVenteRapport.builder()
                        .numeroFacture(v.getNumeroFacture())
                        .dateVente(v.getDateVente())
                        .vendeurNom(v.getVendeur().getNomComplet())
                        .montantHT(v.getMontantHT())
                        .montantReduction(v.getMontantReductions())
                        .montantTTC(v.getMontantTTC())
                        .nombreArticles(v.getLignes().size())
                        .aReduction(v.getMontantReductions().compareTo(BigDecimal.ZERO) > 0)
                        .build())
                .collect(Collectors.toList());

        return RapportVentesDTO.builder()
                .dateDebut(debut)
                .dateFin(fin)
                .typeRapport(typeRapport)
                .nombreVentes(ventesAffichees.size())
                .caTotal(caTotal)
                .totalReductions(totalReductions)
                .caNnet(caTotal.subtract(totalReductions))
                .nombreVentesAvecReduction(ventesAvec.size())
                .caVentesAvecReduction(caAvec)
                .montantReductionsAccordees(totalReductions)
                .nombreVentesSansReduction(ventesSans.size())
                .caVentesSansReduction(caSans)
                .ventes(lignes)
                .build();
    }

    // ══════════════════════════════════════════════════
    // RAPPORT COMPTE DE RÉSULTAT
    // ══════════════════════════════════════════════════

    public RapportCompteResultatDTO getRapportCompteResultat(LocalDate debut, LocalDate fin) {
        log.info("Compte de résultat du {} au {}", debut, fin);

        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT   = fin.atTime(LocalTime.MAX);

       BigDecimal caVentes = venteRepository.sumMontantByPeriode(debutDT, finDT);
        BigDecimal reductions  = getSumReductionsByPeriode(debutDT, finDT);
        BigDecimal caNet       = (caVentes != null ? caVentes : BigDecimal.ZERO)
                                    .subtract(reductions != null ? reductions : BigDecimal.ZERO);
        BigDecimal totalCharges = depenseRepository.sumMontantByPeriode(debut, fin);
        if (totalCharges == null) totalCharges = BigDecimal.ZERO;

        BigDecimal resultat = caNet.subtract(totalCharges);

        // Charges par catégorie
        List<Object[]> rawCats = depenseRepository.sumMontantByCategorieAndPeriode(debut, fin);
        List<RapportCompteResultatDTO.ChargeParCategorie> charges = new ArrayList<>();
        final BigDecimal finalCharges = totalCharges;
        for (Object[] row : rawCats) {
            String     nom     = (String) row[0];
            BigDecimal montant = (BigDecimal) row[1];
            long       nb      = ((Number) row[2]).longValue();
            BigDecimal pct     = finalCharges.compareTo(BigDecimal.ZERO) > 0
                    ? montant.divide(finalCharges, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            charges.add(RapportCompteResultatDTO.ChargeParCategorie.builder()
                    .categorieNom(nom)
                    .montant(montant)
                    .nombreDepenses(nb)
                    .pourcentageDesCharges(pct)
                    .build());
        }

        return RapportCompteResultatDTO.builder()
                .dateDebut(debut)
                .dateFin(fin)
                .caVentes(caVentes != null ? caVentes : BigDecimal.ZERO)
                .totalReductionsAccordees(reductions != null ? reductions : BigDecimal.ZERO)
                .caNet(caNet)
                .totalCharges(totalCharges)
                .chargesParCategorie(charges)
                .resultatNet(resultat)
                .beneficiaire(resultat.compareTo(BigDecimal.ZERO) >= 0)
                .build();
    }

    // ══════════════════════════════════════════════════
    // RAPPORT TRÉSORERIE
    // ══════════════════════════════════════════════════

    public RapportTresorerieDTO getRapportTresorerie(LocalDate debut, LocalDate fin) {
        log.info("Rapport trésorerie du {} au {}", debut, fin);

        LocalDateTime debutDT = debut.atStartOfDay();
        LocalDateTime finDT   = fin.atTime(LocalTime.MAX);

        List<RapportTresorerieDTO.FluxTresorerie> flux = new ArrayList<>();

        // Entrées : ventes
        List<Vente> ventes = venteRepository.findByPeriode(debutDT, finDT);
        for (Vente v : ventes) {
            flux.add(RapportTresorerieDTO.FluxTresorerie.builder()
                    .date(v.getDateVente().toLocalDate())
                    .type("ENTREE")
                    .libelle("Vente — " + v.getNumeroFacture())
                    .categorie("Ventes")
                    .montant(v.getMontantTTC())
                    .reference(v.getNumeroFacture())
                    .enregistrePar(v.getVendeur().getNomComplet())
                    .build());
        }

        // Sorties : dépenses
        List<com.hexalib.api.comptabilite.model.Depense> depenses =
                depenseRepository.findByPeriode(debut, fin);
        for (var d : depenses) {
            flux.add(RapportTresorerieDTO.FluxTresorerie.builder()
                    .date(d.getDateDepense())
                    .type("SORTIE")
                    .libelle(d.getDescription())
                    .categorie(d.getCategorie().getNom())
                    .montant(d.getMontant())
                    .reference(d.getReference())
                    .enregistrePar(d.getEnregistrePar() != null
                            ? d.getEnregistrePar().getNomComplet() : null)
                    .build());
        }

        // Trier chronologiquement
        flux.sort(Comparator.comparing(RapportTresorerieDTO.FluxTresorerie::getDate));

        BigDecimal totalEntrees = flux.stream()
                .filter(f -> "ENTREE".equals(f.getType()))
                .map(RapportTresorerieDTO.FluxTresorerie::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalSorties = flux.stream()
                .filter(f -> "SORTIE".equals(f.getType()))
                .map(RapportTresorerieDTO.FluxTresorerie::getMontant)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return RapportTresorerieDTO.builder()
                .dateDebut(debut)
                .dateFin(fin)
                .totalEntrees(totalEntrees)
                .totalSorties(totalSorties)
                .soldeNet(totalEntrees.subtract(totalSorties))
                .flux(flux)
                .build();
    }

    // ══════════════════════════════════════════════════
    // RAPPORT STOCK VALORISÉ
    // ══════════════════════════════════════════════════

    public RapportStockValoriseDTO getRapportStockValorise() {
        log.info("Rapport stock valorisé");

        List<Livre> tousLivres = livreRepository.findAll();

        BigDecimal valeurTotale = tousLivres.stream()
                .filter(l -> l.getStatut() == Livre.Statut.ACTIF)
                .map(l -> l.getPrixVente().multiply(BigDecimal.valueOf(l.getQuantiteStock())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        int enRupture  = (int) tousLivres.stream().filter(Livre::isEnRupture).count();
        int critiques  = (int) tousLivres.stream()
                .filter(l -> l.isStockCritique() && !l.isEnRupture()).count();

        // Par catégorie
        List<RapportStockValoriseDTO.StockParCategorie> parCategorie = tousLivres.stream()
                .filter(l -> l.getStatut() == Livre.Statut.ACTIF)
                .collect(Collectors.groupingBy(l -> l.getCategorie().getNom()))
                .entrySet().stream()
                .map(e -> {
                    List<Livre> livresCat = e.getValue();
                    BigDecimal valeur = livresCat.stream()
                            .map(l -> l.getPrixVente().multiply(BigDecimal.valueOf(l.getQuantiteStock())))
                            .reduce(BigDecimal.ZERO, BigDecimal::add);
                    int qte = livresCat.stream().mapToInt(Livre::getQuantiteStock).sum();
                    String code = livresCat.get(0).getCategorie().getCode();
                    return RapportStockValoriseDTO.StockParCategorie.builder()
                            .categorieNom(e.getKey())
                            .categorieCode(code)
                            .nombreLivres(livresCat.size())
                            .quantiteTotale(qte)
                            .valeurPrixVente(valeur)
                            .build();
                })
                .sorted(Comparator.comparing(
                        RapportStockValoriseDTO.StockParCategorie::getValeurPrixVente).reversed())
                .collect(Collectors.toList());

        // Livres en rupture
        List<RapportStockValoriseDTO.LivreStockInfo> livresRupture = tousLivres.stream()
                .filter(Livre::isEnRupture)
                .map(this::toLivreStockInfo)
                .collect(Collectors.toList());

        // Livres critiques
        List<RapportStockValoriseDTO.LivreStockInfo> livresCritiques = tousLivres.stream()
                .filter(l -> l.isStockCritique() && !l.isEnRupture())
                .map(this::toLivreStockInfo)
                .collect(Collectors.toList());

        return RapportStockValoriseDTO.builder()
                .dateGeneration(LocalDate.now())
                .valeurTotaleStockPrixVente(valeurTotale)
                .totalLivresEnStock(tousLivres.size())
                .totalLivresEnRupture(enRupture)
                .totalLivresCritiques(critiques)
                .stockParCategorie(parCategorie)
                .livresEnRupture(livresRupture)
                .livresCritiques(livresCritiques)
                .build();
    }

    // ══════════════════════════════════════════════════
    // HELPERS
    // ══════════════════════════════════════════════════

    private BigDecimal sum(List<Vente> ventes,
                           java.util.function.Function<Vente, BigDecimal> mapper) {
        return ventes.stream().map(mapper).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal getSumReductionsByPeriode(LocalDateTime debut, LocalDateTime fin) {
        List<Vente> ventes = venteRepository.findByPeriode(debut, fin);
        return ventes.stream()
                .map(Vente::getMontantReductions)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<DashboardComptaDTO.DepenseParCategorie> buildDepensesParCategorie(
            List<Object[]> raw, BigDecimal total) {
        List<DashboardComptaDTO.DepenseParCategorie> result = new ArrayList<>();
        if (total == null) total = BigDecimal.ZERO;
        final BigDecimal finalTotal = total;
        for (Object[] row : raw) {
            String     nom     = (String) row[0];
            BigDecimal montant = (BigDecimal) row[1];
            long       nb      = ((Number) row[2]).longValue();
            BigDecimal pct     = finalTotal.compareTo(BigDecimal.ZERO) > 0
                    ? montant.divide(finalTotal, 4, RoundingMode.HALF_UP)
                             .multiply(BigDecimal.valueOf(100))
                    : BigDecimal.ZERO;
            result.add(DashboardComptaDTO.DepenseParCategorie.builder()
                    .categorieNom(nom)
                    .montant(montant)
                    .nombreDepenses(nb)
                    .pourcentage(pct)
                    .build());
        }
        return result;
    }

    private RapportStockValoriseDTO.LivreStockInfo toLivreStockInfo(Livre l) {
        return RapportStockValoriseDTO.LivreStockInfo.builder()
                .code(l.getCode())
                .titre(l.getTitre())
                .auteur(l.getAuteur())
                .categorieNom(l.getCategorie().getNom())
                .quantiteStock(l.getQuantiteStock())
                .seuilMinimal(l.getSeuilMinimal())
                .prixVente(l.getPrixVente())
                .build();
    }
}