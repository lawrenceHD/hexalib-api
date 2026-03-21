package com.hexalib.api.rapport.service;

import com.hexalib.api.rapport.dto.*;
import com.hexalib.api.vente.repository.VenteRepository;
import com.hexalib.api.livre.repository.LivreRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional(readOnly = true)
public class RapportService {

    private final StatistiqueService statistiqueService;

    // ==================== RAPPORT JOURNALIER ====================

    public RapportJournalierDTO getRapportJournalier(LocalDate date) {
        log.info("Génération du rapport journalier pour le {}", date);
        LocalDateTime debut = date.atStartOfDay();
        LocalDateTime fin   = date.atTime(LocalTime.MAX);

        StatsPeriodiqueDTO          stats          = statistiqueService.getStatsPeriodique(debut, fin);
        List<PerformanceVendeurDTO> caParVendeur   = statistiqueService.getPerformanceVendeurs(debut, fin);
        List<TopLivreDTO>           topLivres      = statistiqueService.getTopLivres(debut, fin, 5);
        List<TopCategorieDTO>       topCategories  = statistiqueService.getTopCategories(debut, fin, 3);
        List<LivreStockCritiqueDTO> alertesStock   = statistiqueService.getLivresStockCritique();

        return RapportJournalierDTO.builder()
                .date(date)
                .nombreVentes(stats.getNombreVentes())
                .chiffreAffaires(stats.getChiffreAffaires())
                .montantReductions(stats.getMontantReductions())
                .nombreLivresVendus(stats.getNombreLivresVendus())
                .caParVendeur(caParVendeur)
                .topLivres(topLivres)
                .topCategories(topCategories)
                .alertesStock(alertesStock)
                .build();
    }

    // ==================== RAPPORTS PÉRIODIQUES ====================

    public RapportPeriodiqueDTO getRapportHebdomadaire(LocalDate dateFin) {
        return getRapportPeriodique(dateFin.minusDays(6), dateFin, "HEBDOMADAIRE");
    }

    public RapportPeriodiqueDTO getRapportMensuel(LocalDate date) {
        return getRapportPeriodique(
            date.with(TemporalAdjusters.firstDayOfMonth()),
            date.with(TemporalAdjusters.lastDayOfMonth()),
            "MENSUEL"
        );
    }

    public RapportPeriodiqueDTO getRapportAnnuel(int annee) {
        return getRapportPeriodique(
            LocalDate.of(annee, 1, 1),
            LocalDate.of(annee, 12, 31),
            "ANNUEL"
        );
    }

    public RapportPeriodiqueDTO getRapportPersonnalise(LocalDate dateDebut, LocalDate dateFin) {
        return getRapportPeriodique(dateDebut, dateFin, "PERSONNALISE");
    }

    // ==================== DASHBOARDS ====================

    public DashboardAdminDTO getDashboardAdmin() {
        log.info("Génération du dashboard admin");

        LocalDate       aujourdhui  = LocalDate.now();
        LocalDateTime   debutJour   = aujourdhui.atStartOfDay();
        LocalDateTime   finJour     = aujourdhui.atTime(LocalTime.MAX);
        LocalDate       debutMois   = aujourdhui.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime   debutMoisDT = debutMois.atStartOfDay();
        LocalDateTime   finMoisDT   = aujourdhui.atTime(LocalTime.MAX);

        StatsPeriodiqueDTO          statsJour            = statistiqueService.getStatsPeriodique(debutJour, finJour);
        StatsPeriodiqueDTO          statsMois            = statistiqueService.getStatsPeriodique(debutMoisDT, finMoisDT);
        long                        nbStockCritique      = statistiqueService.countLivresStockCritique();
        List<LivreStockCritiqueDTO> livresStockCritique  = statistiqueService.getLivresStockCritique();
        List<EvolutionCADTO>        evolutionCA          = statistiqueService.getEvolutionCA(aujourdhui.minusDays(6), 7);
        List<TopLivreDTO>           top5Livres           = statistiqueService.getTopLivres(debutMoisDT, finMoisDT, 5);
        List<PerformanceVendeurDTO> performanceVendeurs  = statistiqueService.getPerformanceVendeurs(debutMoisDT, finMoisDT);

        return DashboardAdminDTO.builder()
                .caJour(statsJour.getChiffreAffaires())
                .nombreVentesJour(statsJour.getNombreVentes())
                .caMois(statsMois.getChiffreAffaires())
                .nombreVentesMois(statsMois.getNombreVentes())
                .nombreLivresStockCritique(nbStockCritique)
                .livresStockCritique(livresStockCritique)
                .evolutionCA7Jours(evolutionCA)
                .top5LivresMois(top5Livres)
                .performanceVendeurs(performanceVendeurs)
                .totalLivresCatalogue(0L)
                .totalCategories(0L)
                .totalVendeurs(0L)
                .build();
    }

    public DashboardVendeurDTO getDashboardVendeur(String vendeurId) {
        log.info("Génération du dashboard pour le vendeur {}", vendeurId);

        LocalDate     aujourdhui  = LocalDate.now();
        LocalDateTime debutJour   = aujourdhui.atStartOfDay();
        LocalDateTime finJour     = aujourdhui.atTime(LocalTime.MAX);
        LocalDate     debutMois   = aujourdhui.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime debutMoisDT = debutMois.atStartOfDay();
        LocalDateTime finMoisDT   = aujourdhui.atTime(LocalTime.MAX);

        StatsPeriodiqueDTO statsJour  = statistiqueService.getStatsVendeur(vendeurId, debutJour, finJour);
        StatsPeriodiqueDTO statsMois  = statistiqueService.getStatsVendeur(vendeurId, debutMoisDT, finMoisDT);
        List<TopLivreDTO>  topLivres  = statistiqueService.getTopLivresVendeur(vendeurId, debutMoisDT, finMoisDT, 5);
        long               nbCritique = statistiqueService.countLivresStockCritique();

        return DashboardVendeurDTO.builder()
                .mesVentesJour(statsJour.getNombreVentes())
                .monCAJour(statsJour.getChiffreAffaires())
                .mesVentesMois(statsMois.getNombreVentes())
                .monCAMois(statsMois.getChiffreAffaires())
                .mesMeilleuresVentes(topLivres)
                .nombreLivresStockCritique(nbCritique)
                .monClassement(null)
                .objectifMensuel(null)
                .tauxAtteinte(null)
                .build();
    }

    // ==================== MÉTHODES PRIVÉES ====================

    private RapportPeriodiqueDTO getRapportPeriodique(LocalDate dateDebut, LocalDate dateFin, String typePeriode) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin   = dateFin.atTime(LocalTime.MAX);

        StatsPeriodiqueDTO          stats               = statistiqueService.getStatsPeriodique(debut, fin);
        long                        joursEcart          = java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
        LocalDateTime               debutPrecedent      = dateDebut.minusDays(joursEcart).atStartOfDay();
        LocalDateTime               finPrecedent        = dateDebut.minusDays(1).atTime(LocalTime.MAX);
        EvolutionComparativeDTO     evolution           = statistiqueService.comparerPeriodes(debut, fin, debutPrecedent, finPrecedent);
        List<EvolutionCADTO>        evolutionCA         = statistiqueService.getEvolutionCA(dateFin.minusDays(6), 7);
        List<TopLivreDTO>           topLivres           = statistiqueService.getTopLivres(debut, fin, 10);
        List<TopCategorieDTO>       topCategories       = statistiqueService.getTopCategories(debut, fin, 5);
        List<PerformanceVendeurDTO> performanceVendeurs = statistiqueService.getPerformanceVendeurs(debut, fin);
        AnalyseReductionsDTO        analyseReductions   = statistiqueService.getAnalyseReductions(debut, fin);
        List<RotationStockDTO>      rotationStock       = statistiqueService.getRotationStock(debut, fin);

        return RapportPeriodiqueDTO.builder()
                .dateDebut(dateDebut)
                .dateFin(dateFin)
                .periode(typePeriode)
                .nombreVentes(stats.getNombreVentes())
                .chiffreAffaires(stats.getChiffreAffaires())
                .montantReductions(stats.getMontantReductions())
                .nombreLivresVendus(stats.getNombreLivresVendus())
                .margeBeneficiaire(stats.getMargeBeneficiaire())
                .evolutionCA(evolution.getEvolutionCA())
                .evolutionNombreVentes(evolution.getEvolutionNombreVentes())
                .evolutionCA7Jours(evolutionCA)
                .topLivres(topLivres)
                .topCategories(topCategories)
                .performanceVendeurs(performanceVendeurs)
                .analyseReductions(analyseReductions)
                .rotationStock(rotationStock)
                .build();
    }
}