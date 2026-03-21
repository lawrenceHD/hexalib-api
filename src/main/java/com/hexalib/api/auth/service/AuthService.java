package com.hexalib.api.auth.service;

import com.hexalib.api.auth.dto.LoginRequest;
import com.hexalib.api.auth.dto.LoginResponse;
import com.hexalib.api.auth.dto.RegisterRequest;
import com.hexalib.api.auth.dto.UserDto;
import com.hexalib.api.auth.model.RefreshToken;
import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.RefreshTokenRepository;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.auth.security.JwtTokenProvider;
import com.hexalib.api.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository         userRepository;
    private final PasswordEncoder        passwordEncoder;
    private final AuthenticationManager  authenticationManager;
    private final JwtTokenProvider       tokenProvider;
    private final RefreshTokenRepository refreshTokenRepository;
    private final LoginAttemptService    loginAttemptService;
    private final TokenBlacklistService  tokenBlacklistService;

    // ── Register ──────────────────────────────────────────────────────────────

    @Transactional
    public UserDto register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        User user = new User();
        user.setNomComplet(request.getNomComplet());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatut(User.Statut.ACTIF);
        user.setDateCreation(LocalDateTime.now());
        user.setPremiereConnexion(true);
        user.setMotDePasseTemporaire(false);

        User saved = userRepository.save(user);
        log.info("Nouvel utilisateur créé : {} ({})", saved.getEmail(), saved.getRole());
        return UserDto.fromEntity(saved);
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse login(LoginRequest request) {
        String email = request.getEmail().toLowerCase().trim();

        // 1. Vérifier si le compte est bloqué
        if (loginAttemptService.isBlocked(email)) {
            LocalDateTime until = loginAttemptService.getBlockedUntil(email);
            throw new BadRequestException(
                "Compte temporairement bloqué suite à trop de tentatives échouées. " +
                "Réessayez après " + until.toLocalTime().toString().substring(0, 5) + "."
            );
        }

        // 2. Tenter l'authentification
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.getPassword())
            );
        } catch (BadCredentialsException e) {
            loginAttemptService.loginFailed(email);
            int remaining = loginAttemptService.getRemainingAttempts(email);
            String msg = remaining > 0
                ? "Email ou mot de passe incorrect. " + remaining + " tentative(s) restante(s)."
                : "Compte bloqué pendant 15 minutes suite à trop de tentatives échouées.";
            throw new BadRequestException(msg);
        } catch (DisabledException e) {
            throw new BadRequestException("Ce compte est désactivé. Contactez un administrateur.");
        }

        // 3. Connexion réussie
        loginAttemptService.loginSucceeded(email);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 4. Générer access token
        String accessToken = tokenProvider.generateToken(authentication);

        // 5. Récupérer l'utilisateur et mettre à jour dernière connexion
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        user.setDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);

        // 6. Créer refresh token
        RefreshToken refreshToken = tokenProvider.createRefreshToken(user);

        log.info("Connexion réussie : {}", email);

        return LoginResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken.getToken())
                .type("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .user(UserDto.fromEntity(user))
                .build();
    }

    // ── Refresh Token ─────────────────────────────────────────────────────────

    @Transactional
    public LoginResponse refreshToken(String refreshTokenStr) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(refreshTokenStr)
            .orElseThrow(() -> new BadRequestException("Refresh token invalide ou expiré"));

        if (!refreshToken.isValid()) {
            throw new BadRequestException("Refresh token expiré ou révoqué. Veuillez vous reconnecter.");
        }

        User user = refreshToken.getUser();

        if (user.getStatut() == User.Statut.INACTIF) {
            throw new BadRequestException("Ce compte est désactivé.");
        }

        // Générer un nouveau access token
        String newAccessToken = tokenProvider.generateToken(user.getEmail());

        // Rotation du refresh token (sécurité)
        refreshTokenRepository.revokeAllUserTokens(user);
        RefreshToken newRefreshToken = tokenProvider.createRefreshToken(user);

        log.info("Token rafraîchi pour : {}", user.getEmail());

        return LoginResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken.getToken())
                .type("Bearer")
                .expiresIn(tokenProvider.getExpirationMs())
                .user(UserDto.fromEntity(user))
                .build();
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @Transactional
    public void logout(String accessToken) {
        // Blacklister l'access token
        tokenBlacklistService.blacklist(accessToken);

        // Révoquer tous les refresh tokens de l'utilisateur connecté
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null) {
            userRepository.findByEmail(auth.getName())
                .ifPresent(user -> {
                    refreshTokenRepository.revokeAllUserTokens(user);
                    log.info("Déconnexion et révocation des tokens pour : {}", user.getEmail());
                });
        }

        SecurityContextHolder.clearContext();
    }

    // ── Current user ──────────────────────────────────────────────────────────

    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();
        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));
        return UserDto.fromEntity(user);
    }
}