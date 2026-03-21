package com.hexalib.api.auth.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Blacklist des access tokens invalidés (logout).
 * Stockage en mémoire avec nettoyage automatique (max 10 000 entrées).
 */
@Service
@Slf4j
public class TokenBlacklistService {

    private static final int MAX_SIZE = 10_000;

    // LRU cache — supprime automatiquement les entrées les plus anciennes
    private final Map<String, Boolean> blacklist = Collections.synchronizedMap(
        new LinkedHashMap<>(MAX_SIZE, 0.75f, false) {
            @Override
            protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                return size() > MAX_SIZE;
            }
        }
    );

    public void blacklist(String token) {
        blacklist.put(token, Boolean.TRUE);
        log.debug("Token ajouté à la blacklist");
    }

    public boolean isBlacklisted(String token) {
        return blacklist.containsKey(token);
    }
}