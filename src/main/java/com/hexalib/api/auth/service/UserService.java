package com.hexalib.api.auth.service;

import com.hexalib.api.auth.dto.*;
import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import com.hexalib.api.vente.repository.VenteRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final VenteRepository venteRepository;

    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789@#$%&*";
    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Mettre à jour son propre profil
     */
    public UserDto updateProfil(UpdateProfilRequest request) {
        User user = getCurrentUser();
        
        log.info("Mise à jour du profil pour l'utilisateur: {}", user.getEmail());

        // Vérifier si l'email existe déjà (pour un autre utilisateur)
        if (!user.getEmail().equals(request.getEmail())) {
            if (userRepository.findByEmail(request.getEmail()).isPresent()) {
                throw new BadRequestException("Cet email est déjà utilisé");
            }
        }

        user.setNomComplet(request.getNomComplet());
        user.setEmail(request.getEmail());

        User updated = userRepository.save(user);
        return mapToResponse(updated);
    }

    /**
     * Changer son mot de passe
     */
    public void changePassword(ChangePasswordRequest request) {
        User user = getCurrentUser();
        
        log.info("Changement de mot de passe pour l'utilisateur: {}", user.getEmail());

        // Vérifier l'ancien mot de passe
        if (!passwordEncoder.matches(request.getAncienMotDePasse(), user.getPassword())) {
            throw new BadRequestException("L'ancien mot de passe est incorrect");
        }

        // Vérifier que le nouveau mot de passe est différent
        if (passwordEncoder.matches(request.getNouveauMotDePasse(), user.getPassword())) {
            throw new BadRequestException("Le nouveau mot de passe doit être différent de l'ancien");
        }

        // Vérifier la confirmation
        if (!request.getNouveauMotDePasse().equals(request.getConfirmationMotDePasse())) {
            throw new BadRequestException("La confirmation du mot de passe ne correspond pas");
        }

        // Changer le mot de passe
        user.setPassword(passwordEncoder.encode(request.getNouveauMotDePasse()));
        user.setPremiereConnexion(false);
        user.setMotDePasseTemporaire(false);

        userRepository.save(user);
        log.info("Mot de passe changé avec succès pour: {}", user.getEmail());
    }

    /**
     * Réinitialiser le mot de passe d'un utilisateur (Admin uniquement)
     */
    public ResetPasswordResponse resetPassword(String userId) {
        log.info("Réinitialisation du mot de passe pour l'utilisateur ID: {}", userId);

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Générer un nouveau mot de passe temporaire
        String motDePasseTemp = generateSecurePassword();

        user.setPassword(passwordEncoder.encode(motDePasseTemp));
        user.setMotDePasseTemporaire(true);
        user.setPremiereConnexion(true);

        userRepository.save(user);

        log.info("Mot de passe réinitialisé pour: {}", user.getEmail());

        // TODO: Envoyer un email à l'utilisateur avec le nouveau mot de passe

        return ResetPasswordResponse.builder()
                .motDePasseTemporaire(motDePasseTemp)
                .message("Mot de passe réinitialisé avec succès. Le vendeur doit changer son mot de passe à la prochaine connexion.")
                .build();
    }

    /**
     * Marquer la dernière connexion
     */
    public void updateDerniereConnexion(String userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setDerniereConnexion(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Obtenir les statistiques du vendeur connecté
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getMyStats() {
        User user = getCurrentUser();
        return getUserStats(user.getId());
    }

    /**
     * Obtenir les statistiques d'un vendeur (Admin)
     */
    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(String userId) {
        LocalDate today = LocalDate.now();
        YearMonth currentMonth = YearMonth.now();
        LocalDate startOfMonth = currentMonth.atDay(1);

        // Stats du jour
        long ventesJour = venteRepository.countByVendeurAndDate(userId, today);
        BigDecimal caJour = venteRepository.sumMontantByVendeurAndDate(userId, today);

        // Stats du mois
        long ventesMois = venteRepository.countByVendeurBetween(
                userId,
                startOfMonth.atStartOfDay(),
                LocalDateTime.now()
        );
        BigDecimal caMois = venteRepository.sumMontantByVendeurBetween(
                userId,
                startOfMonth.atStartOfDay(),
                LocalDateTime.now()
        );

        return UserStatsResponse.builder()
                .nombreVentesAujourdhui(ventesJour)
                .caAujourdhui(caJour != null ? caJour : BigDecimal.ZERO)
                .nombreVentesMois(ventesMois)
                .caMois(caMois != null ? caMois : BigDecimal.ZERO)
                .build();
    }

    /**
     * Générer un mot de passe sécurisé
     */
    public String generateSecurePassword() {
        StringBuilder password = new StringBuilder(12);
        
        // Au moins une majuscule
        password.append((char) (RANDOM.nextInt(26) + 'A'));
        // Au moins une minuscule
        password.append((char) (RANDOM.nextInt(26) + 'a'));
        // Au moins un chiffre
        password.append((char) (RANDOM.nextInt(10) + '0'));
        // Au moins un caractère spécial
        password.append("@#$%&*".charAt(RANDOM.nextInt(6)));

        // Compléter avec des caractères aléatoires
        for (int i = 4; i < 12; i++) {
            password.append(CHARACTERS.charAt(RANDOM.nextInt(CHARACTERS.length())));
        }

        // Mélanger les caractères
        return shuffleString(password.toString());
    }

    private String shuffleString(String string) {
        char[] characters = string.toCharArray();
        for (int i = characters.length - 1; i > 0; i--) {
            int j = RANDOM.nextInt(i + 1);
            char temp = characters[i];
            characters[i] = characters[j];
            characters[j] = temp;
        }
        return new String(characters);
    }

    /**
     * Récupérer l'utilisateur connecté
     */
    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
    }

    /**
     * Mapper vers Response
     */
    private UserDto mapToResponse(User user) {
        return UserDto.builder()
                .id(user.getId())
                .nomComplet(user.getNomComplet())
                .email(user.getEmail())
                .role(user.getRole())
                .statut(user.getStatut())
                .premiereConnexion(user.getPremiereConnexion())
                .derniereConnexion(user.getDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .build();
    }
}