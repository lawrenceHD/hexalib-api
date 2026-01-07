package com.hexalib.api.common.util;

import java.util.Random;

public class CodeGenerator {

    private static final Random RANDOM = new Random();

    /**
     * Génère un code de catégorie (4 premières lettres + 3 chiffres)
     * Exemple: "ROMA123" pour "Romans"
     */
    public static String generateCategorieCode(String nom) {
        // Prendre les 4 premières lettres (en majuscules, sans accents)
        String prefix = nom.trim()
                .toUpperCase()
                .replaceAll("[ÀÂÄÁÃ]", "A")
                .replaceAll("[ÈÊËÉ]", "E")
                .replaceAll("[ÌÎÏÍ]", "I")
                .replaceAll("[ÒÔÖÓÕ]", "O")
                .replaceAll("[ÙÛÜÚ]", "U")
                .replaceAll("[^A-Z]", "")
                .substring(0, Math.min(4, nom.length()));

        // Compléter avec des 'X' si moins de 4 lettres
        while (prefix.length() < 4) {
            prefix += "X";
        }

        // Ajouter 3 chiffres aléatoires
        String suffix = String.format("%03d", RANDOM.nextInt(1000));

        return prefix + suffix;
    }

    /**
     * Génère un code de livre (code catégorie + tiret + 3 chiffres)
     * Exemple: "ROMA123-001"
     */
    public static String generateLivreCode(String codeCategorie) {
        String suffix = String.format("%03d", RANDOM.nextInt(1000));
        return codeCategorie + "-" + suffix;
    }

    /**
     * Génère un numéro de facture (FAC-YYYYMMDD-XXX)
     * Exemple: "FAC-20260105-001"
     */
    public static String generateNumeroFacture() {
        String date = java.time.LocalDate.now()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyyMMdd"));
        String suffix = String.format("%03d", RANDOM.nextInt(1000));
        return "FAC-" + date + "-" + suffix;
    }
}