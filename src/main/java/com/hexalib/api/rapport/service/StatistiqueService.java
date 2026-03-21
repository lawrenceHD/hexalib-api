package com.hexalib.api.rapport.service;

import com.hexalib.api.rapport.dto.*;
import com.hexalib.api.rapport.repository.RapportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class StatistiqueService {

    private final RapportRepository rapportRepository;

    // ==================== STATS GLOBALES ====================

    public StatsPeriodiqueDTO getStatsPeriodique(LocalDateTime debut, LocalDateTime fin) {
        long       nombreVentes = rapportRepository.countVentesByPeriode(debut, fin);
        BigDecimal ca           = rapportRepository.sumCAByPeriode(debut, fin);
        BigDecimal reductions   = rapportRepository.sumReductionsByPeriode(debut, fin);
        long       livresVendus = rapportRepository.sumQuantiteLivresVendus(debut, fin);
        BigDecimal marge        = rapportRepository.getMargeBeneficiaire(debut, fin);

        return StatsPeriodiqueDTO.builder()
                .nombreVentes(nombreVentes)
                .chiffreAffaires(ca)
                .montantReductions(reductions)
                .nombreLivresVendus(livresVendus)
                .margeBeneficiaire(marge)
                .panierMoyen(nombreVentes > 0
                        ? ca.divide(BigDecimal.valueOf(nombreVentes), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .build();
    }

    // ==================== STATS PAR VENDEUR ====================

    /**
     * Stats d'un vendeur spécifique sur une période
     */
    public StatsPeriodiqueDTO getStatsVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        log.debug("Calcul stats vendeur {} du {} au {}", vendeurId, debut, fin);

        long       nombreVentes = rapportRepository.countVentesByVendeur(vendeurId, debut, fin);
        BigDecimal ca           = rapportRepository.sumCAByVendeur(vendeurId, debut, fin);
        BigDecimal reductions   = rapportRepository.sumReductionsByVendeur(vendeurId, debut, fin);
        long       livresVendus = rapportRepository.sumQuantiteLivresVendusByVendeur(vendeurId, debut, fin);

        return StatsPeriodiqueDTO.builder()
                .nombreVentes(nombreVentes)
                .chiffreAffaires(ca)
                .montantReductions(reductions)
                .nombreLivresVendus(livresVendus)
                .margeBeneficiaire(BigDecimal.ZERO) // non calculé par vendeur
                .panierMoyen(nombreVentes > 0
                        ? ca.divide(BigDecimal.valueOf(nombreVentes), 2, RoundingMode.HALF_UP)
                        : BigDecimal.ZERO)
                .build();
    }

    /**
     * Top livres d'un vendeur spécifique
     */
    public List<TopLivreDTO> getTopLivresVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin, int limit) {
        List<TopLivreDTO> topLivres = rapportRepository.getTopLivresByVendeur(vendeurId, debut, fin);
        return IntStream.range(0, Math.min(limit, topLivres.size()))
                .mapToObj(i -> {
                    TopLivreDTO livre = topLivres.get(i);
                    livre.setRang(i + 1);
                    return livre;
                })
                .toList();
    }

    // ==================== ÉVOLUTION CA ====================

    public List<EvolutionCADTO> getEvolutionCA(LocalDate dateDebut, int nombreJours) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin   = dateDebut.plusDays(nombreJours).atTime(LocalTime.MAX);
        return rapportRepository.getEvolutionCA(debut, fin);
    }

    // ==================== TOP LIVRES / CATÉGORIES ====================

    public List<TopLivreDTO> getTopLivres(LocalDateTime debut, LocalDateTime fin, int limit) {
        List<TopLivreDTO> topLivres = rapportRepository.getTopLivres(debut, fin);
        return IntStream.range(0, Math.min(limit, topLivres.size()))
                .mapToObj(i -> {
                    TopLivreDTO livre = topLivres.get(i);
                    livre.setRang(i + 1);
                    return livre;
                })
                .toList();
    }

    public List<TopCategorieDTO> getTopCategories(LocalDateTime debut, LocalDateTime fin, int limit) {
        List<TopCategorieDTO> topCategories = rapportRepository.getTopCategories(debut, fin);
        return IntStream.range(0, Math.min(limit, topCategories.size()))
                .mapToObj(i -> {
                    TopCategorieDTO cat = topCategories.get(i);
                    cat.setRang(i + 1);
                    return cat;
                })
                .toList();
    }

    // ==================== PERFORMANCE VENDEURS ====================

    public List<PerformanceVendeurDTO> getPerformanceVendeurs(LocalDateTime debut, LocalDateTime fin) {
        List<PerformanceVendeurDTO> performances = rapportRepository.getPerformanceVendeurs(debut, fin);
        return IntStream.range(0, performances.size())
                .mapToObj(i -> {
                    PerformanceVendeurDTO perf = performances.get(i);
                    perf.setRang(i + 1);
                    return perf;
                })
                .toList();
    }

    // ==================== STOCK ====================

    public List<LivreStockCritiqueDTO> getLivresStockCritique() {
        return rapportRepository.getLivresStockCritique();
    }

    public long countLivresStockCritique() {
        return rapportRepository.countLivresStockCritique();
    }

    // ==================== RÉDUCTIONS ====================

    public AnalyseReductionsDTO getAnalyseReductions(LocalDateTime debut, LocalDateTime fin) {
        long       totalVentes          = rapportRepository.countVentesByPeriode(debut, fin);
        BigDecimal montantTotal         = rapportRepository.sumReductionsByPeriode(debut, fin);
        long       ventesAvecReduction  = rapportRepository.countVentesAvecReduction(debut, fin);
        BigDecimal moyenne              = rapportRepository.getReductionMoyenne(debut, fin);
        BigDecimal maximale             = rapportRepository.getReductionMaximale(debut, fin);

        BigDecimal pourcentage = totalVentes > 0
                ? BigDecimal.valueOf(ventesAvecReduction)
                        .divide(BigDecimal.valueOf(totalVentes), 4, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return AnalyseReductionsDTO.builder()
                .montantTotalReductions(montantTotal)
                .nombreVentesAvecReduction(ventesAvecReduction)
                .pourcentageVentesAvecReduction(pourcentage)
                .reductionMoyenne(moyenne)
                .reductionMaximale(maximale)
                .build();
    }

    // ==================== ROTATION STOCK ====================

    public List<RotationStockDTO> getRotationStock(LocalDateTime debut, LocalDateTime fin) {
        List<RotationStockDTO> rotations = rapportRepository.getRotationStock(debut, fin);
        rotations.forEach(r -> {
            if (r.getStockActuel() > 0) {
                BigDecimal taux = BigDecimal.valueOf(r.getQuantiteVendue())
                        .divide(BigDecimal.valueOf(r.getStockActuel()), 2, RoundingMode.HALF_UP)
                        .multiply(BigDecimal.valueOf(100));
                r.setTauxRotation(taux);
            } else {
                r.setTauxRotation(BigDecimal.ZERO);
            }
        });
        return rotations;
    }

    // ==================== COMPARAISON PÉRIODES ====================

    public EvolutionComparativeDTO comparerPeriodes(LocalDateTime debut1, LocalDateTime fin1,
                                                     LocalDateTime debut2, LocalDateTime fin2) {
        BigDecimal ca1     = rapportRepository.sumCAByPeriode(debut1, fin1);
        BigDecimal ca2     = rapportRepository.sumCAByPeriode(debut2, fin2);
        long       ventes1 = rapportRepository.countVentesByPeriode(debut1, fin1);
        long       ventes2 = rapportRepository.countVentesByPeriode(debut2, fin2);

        BigDecimal evolutionCA = ca2.compareTo(BigDecimal.ZERO) > 0
                ? ca1.subtract(ca2).divide(ca2, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        return EvolutionComparativeDTO.builder()
                .evolutionCA(evolutionCA)
                .evolutionNombreVentes(ventes1 - ventes2)
                .build();
    }
}