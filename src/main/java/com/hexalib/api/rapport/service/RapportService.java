package com.hexalib.api.rapport.service;

import com.hexalib.api.rapport.dto.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    /**
     * Rapport de clôture journalière
     */
    public RapportJournalierDTO getRapportJournalier(LocalDate date) {
        log.info("Génération du rapport journalier pour le {}", date);

        LocalDateTime debut = date.atStartOfDay();
        LocalDateTime fin = date.atTime(LocalTime.MAX);

        // Stats de base
        StatsPeriodiqueDTO stats = statistiqueService.getStatsPeriodique(debut, fin);

        // CA par vendeur
        List<PerformanceVendeurDTO> caParVendeur = statistiqueService.getPerformanceVendeurs(debut, fin);

        // Top 5 livres
        List<TopLivreDTO> topLivres = statistiqueService.getTopLivres(debut, fin, 5);

        // Top 3 catégories
        List<TopCategorieDTO> topCategories = statistiqueService.getTopCategories(debut, fin, 3);

        // Alertes stock
        List<LivreStockCritiqueDTO> alertesStock = statistiqueService.getLivresStockCritique();

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

    /**
     * Rapport hebdomadaire
     */
    public RapportPeriodiqueDTO getRapportHebdomadaire(LocalDate dateFin) {
        log.info("Génération du rapport hebdomadaire se terminant le {}", dateFin);

        LocalDate dateDebut = dateFin.minusDays(6); // 7 jours
        return getRapportPeriodique(dateDebut, dateFin, "HEBDOMADAIRE");
    }

    /**
     * Rapport mensuel
     */
    public RapportPeriodiqueDTO getRapportMensuel(LocalDate date) {
        log.info("Génération du rapport mensuel pour {}", date);

        LocalDate dateDebut = date.with(TemporalAdjusters.firstDayOfMonth());
        LocalDate dateFin = date.with(TemporalAdjusters.lastDayOfMonth());

        return getRapportPeriodique(dateDebut, dateFin, "MENSUEL");
    }

    /**
     * Rapport annuel
     */
    public RapportPeriodiqueDTO getRapportAnnuel(int annee) {
        log.info("Génération du rapport annuel pour {}", annee);

        LocalDate dateDebut = LocalDate.of(annee, 1, 1);
        LocalDate dateFin = LocalDate.of(annee, 12, 31);

        return getRapportPeriodique(dateDebut, dateFin, "ANNUEL");
    }

    /**
     * Rapport personnalisé
     */
    public RapportPeriodiqueDTO getRapportPersonnalise(LocalDate dateDebut, LocalDate dateFin) {
        log.info("Génération du rapport personnalisé du {} au {}", dateDebut, dateFin);
        return getRapportPeriodique(dateDebut, dateFin, "PERSONNALISE");
    }

    /**
     * Dashboard Admin
     */
    public DashboardAdminDTO getDashboardAdmin() {
        log.info("Génération du dashboard admin");

        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutJour = aujourdhui.atStartOfDay();
        LocalDateTime finJour = aujourdhui.atTime(LocalTime.MAX);

        // KPIs du jour
        StatsPeriodiqueDTO statsJour = statistiqueService.getStatsPeriodique(debutJour, finJour);

        // KPIs du mois
        LocalDate debutMois = aujourdhui.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime debutMoisDT = debutMois.atStartOfDay();
        LocalDateTime finMoisDT = aujourdhui.atTime(LocalTime.MAX);
        StatsPeriodiqueDTO statsMois = statistiqueService.getStatsPeriodique(debutMoisDT, finMoisDT);

        // Stock critique
        long nbStockCritique = statistiqueService.countLivresStockCritique();
        List<LivreStockCritiqueDTO> livresStockCritique = statistiqueService.getLivresStockCritique();

        // Évolution CA (7 derniers jours)
        LocalDate il7Jours = aujourdhui.minusDays(6);
        List<EvolutionCADTO> evolutionCA = statistiqueService.getEvolutionCA(il7Jours, 7);

        // Top 5 livres du mois
        List<TopLivreDTO> top5Livres = statistiqueService.getTopLivres(debutMoisDT, finMoisDT, 5);

        // Performance vendeurs
        List<PerformanceVendeurDTO> performanceVendeurs = statistiqueService.getPerformanceVendeurs(debutMoisDT, finMoisDT);

        // TODO: Statistiques globales (nombre total livres, catégories, vendeurs)
        // Pour l'instant mis à 0, à implémenter si nécessaire

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
                .totalLivresCatalogue(0L) // À implémenter
                .totalCategories(0L) // À implémenter
                .totalVendeurs(0L) // À implémenter
                .build();
    }

    /**
     * Dashboard Vendeur
     */
    public DashboardVendeurDTO getDashboardVendeur(String vendeurId) {
        log.info("Génération du dashboard pour le vendeur {}", vendeurId);

        LocalDate aujourdhui = LocalDate.now();
        LocalDateTime debutJour = aujourdhui.atStartOfDay();
        LocalDateTime finJour = aujourdhui.atTime(LocalTime.MAX);

        LocalDate debutMois = aujourdhui.with(TemporalAdjusters.firstDayOfMonth());
        LocalDateTime debutMoisDT = debutMois.atStartOfDay();
        LocalDateTime finMoisDT = aujourdhui.atTime(LocalTime.MAX);

        // Stats du jour du vendeur
        StatsPeriodiqueDTO statsJour = getStatsVendeur(vendeurId, debutJour, finJour);

        // Stats du mois du vendeur
        StatsPeriodiqueDTO statsMois = getStatsVendeur(vendeurId, debutMoisDT, finMoisDT);

        // Top 5 meilleures ventes du vendeur
        List<TopLivreDTO> mesMeilleuresVentes = getTopLivresVendeur(vendeurId, debutMoisDT, finMoisDT, 5);

        // Stock critique (info)
        long nbStockCritique = statistiqueService.countLivresStockCritique();

        // TODO: Classement du vendeur parmi tous les vendeurs
        Integer classement = null; // À implémenter

        return DashboardVendeurDTO.builder()
                .mesVentesJour(statsJour.getNombreVentes())
                .monCAJour(statsJour.getChiffreAffaires())
                .mesVentesMois(statsMois.getNombreVentes())
                .monCAMois(statsMois.getChiffreAffaires())
                .mesMeilleuresVentes(mesMeilleuresVentes)
                .nombreLivresStockCritique(nbStockCritique)
                .monClassement(classement)
                .objectifMensuel(null) // À implémenter si objectifs définis
                .tauxAtteinte(null) // À implémenter
                .build();
    }

    // ==================== MÉTHODES PRIVÉES ====================

    /**
     * Générer un rapport périodique générique
     */
    private RapportPeriodiqueDTO getRapportPeriodique(LocalDate dateDebut, LocalDate dateFin, String typePeriode) {
        LocalDateTime debut = dateDebut.atStartOfDay();
        LocalDateTime fin = dateFin.atTime(LocalTime.MAX);

        // Stats de base
        StatsPeriodiqueDTO stats = statistiqueService.getStatsPeriodique(debut, fin);

        // Comparaison avec période précédente
        long joursEcart = java.time.temporal.ChronoUnit.DAYS.between(dateDebut, dateFin) + 1;
        LocalDateTime debutPrecedent = dateDebut.minusDays(joursEcart).atStartOfDay();
        LocalDateTime finPrecedent = dateDebut.minusDays(1).atTime(LocalTime.MAX);

        EvolutionComparativeDTO evolution = statistiqueService.comparerPeriodes(debut, fin, debutPrecedent, finPrecedent);

        // Évolution CA (7 derniers jours de la période)
        LocalDate dateDebutEvolution = dateFin.minusDays(6);
        List<EvolutionCADTO> evolutionCA = statistiqueService.getEvolutionCA(dateDebutEvolution, 7);

        // Top livres et catégories
        List<TopLivreDTO> topLivres = statistiqueService.getTopLivres(debut, fin, 10);
        List<TopCategorieDTO> topCategories = statistiqueService.getTopCategories(debut, fin, 5);

        // Performance vendeurs
        List<PerformanceVendeurDTO> performanceVendeurs = statistiqueService.getPerformanceVendeurs(debut, fin);

        // Analyse réductions
        AnalyseReductionsDTO analyseReductions = statistiqueService.getAnalyseReductions(debut, fin);

        // Rotation stock
        List<RotationStockDTO> rotationStock = statistiqueService.getRotationStock(debut, fin);

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

    /**
     * Stats pour un vendeur spécifique (à implémenter dans RapportRepository)
     */
    private StatsPeriodiqueDTO getStatsVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin) {
        // TODO: Créer une méthode dans RapportRepository pour stats par vendeur
        // Pour l'instant, retourner des valeurs par défaut
        return StatsPeriodiqueDTO.builder()
                .nombreVentes(0L)
                .chiffreAffaires(java.math.BigDecimal.ZERO)
                .montantReductions(java.math.BigDecimal.ZERO)
                .nombreLivresVendus(0L)
                .margeBeneficiaire(java.math.BigDecimal.ZERO)
                .panierMoyen(java.math.BigDecimal.ZERO)
                .build();
    }

    /**
     * Top livres vendus par un vendeur spécifique
     */
    private List<TopLivreDTO> getTopLivresVendeur(String vendeurId, LocalDateTime debut, LocalDateTime fin, int limit) {
        // TODO: Créer une méthode dans RapportRepository
        return List.of();
    }
}