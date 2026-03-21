package com.hexalib.api.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Gère la limitation des tentatives de connexion (5 max, blocage 15 min).
 * Stockage en mémoire — suffisant pour un seul serveur.
 */
@Service
@Slf4j
public class LoginAttemptService {

    private static final int MAX_ATTEMPTS      = 5;
    private static final int BLOCK_DURATION_MIN = 15;

    // email → [nbTentatives, heureDernierEchec]
    private final ConcurrentHashMap<String, int[]>            attempts   = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime>    blockedUntil = new ConcurrentHashMap<>();

    /** Appelé après un échec d'authentification */
    public void loginFailed(String email) {
        String key = email.toLowerCase();

        int[] count = attempts.getOrDefault(key, new int[]{0});
        count[0]++;
        attempts.put(key, count);

        if (count[0] >= MAX_ATTEMPTS) {
            LocalDateTime unblockTime = LocalDateTime.now().plusMinutes(BLOCK_DURATION_MIN);
            blockedUntil.put(key, unblockTime);
            log.warn("Compte bloqué suite à {} tentatives échouées : {} — débloqué à {}", MAX_ATTEMPTS, email, unblockTime);
        }
    }

    /** Appelé après une connexion réussie — réinitialise le compteur */
    public void loginSucceeded(String email) {
        String key = email.toLowerCase();
        attempts.remove(key);
        blockedUntil.remove(key);
    }

    /** Vérifie si le compte est actuellement bloqué */
    public boolean isBlocked(String email) {
        String key = email.toLowerCase();
        LocalDateTime until = blockedUntil.get(key);

        if (until == null) return false;

        if (LocalDateTime.now().isAfter(until)) {
            // Déblocage automatique après expiration
            attempts.remove(key);
            blockedUntil.remove(key);
            return false;
        }
        return true;
    }

    /** Retourne le nombre de tentatives restantes */
    public int getRemainingAttempts(String email) {
        int used = attempts.getOrDefault(email.toLowerCase(), new int[]{0})[0];
        return Math.max(0, MAX_ATTEMPTS - used);
    }

    /** Retourne l'heure de déblocage si bloqué */
    public LocalDateTime getBlockedUntil(String email) {
        return blockedUntil.get(email.toLowerCase());
    }
}