package com.hexalib.api.auth.service;

import com.hexalib.api.auth.dto.*;
import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.common.exception.BadRequestException;
import com.hexalib.api.common.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class AdminUserService {

    private final UserRepository  userRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserService     userService; // pour generateSecurePassword()

    // ── Liste paginée + recherche ─────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserPageResponse getUsers(int page, int size, String search, User.Role role, User.Statut statut) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("dateCreation").descending());

        Page<User> usersPage;

        // Filtre combiné côté JPQL via une query custom
        usersPage = userRepository.findWithFilters(search, role, statut, pageable);

        List<UserDto> dtos = usersPage.getContent().stream()
                .map(UserDto::fromEntity)
                .collect(Collectors.toList());

        return UserPageResponse.builder()
                .users(dtos)
                .page(page)
                .size(size)
                .total(usersPage.getTotalElements())
                .totalPages(usersPage.getTotalPages())
                .build();
    }

    // ── Obtenir un utilisateur ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserDto getUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));
        return UserDto.fromEntity(user);
    }

    // ── Créer un utilisateur ──────────────────────────────────────────────────

    public UserDto createUser(CreateUserRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        String rawPassword;
        boolean isTemp = false;

        if (StringUtils.hasText(request.getPassword())) {
            rawPassword = request.getPassword();
        } else {
            rawPassword = userService.generateSecurePassword();
            isTemp = true;
        }

        User user = new User();
        user.setNomComplet(request.getNomComplet());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setPassword(passwordEncoder.encode(rawPassword));
        user.setRole(request.getRole());
        user.setStatut(User.Statut.ACTIF);
        user.setDateCreation(LocalDateTime.now());
        user.setPremiereConnexion(true);
        user.setMotDePasseTemporaire(isTemp);

        User saved = userRepository.save(user);
        log.info("Utilisateur créé par admin : {} ({})", saved.getEmail(), saved.getRole());

        // TODO: envoyer email avec mot de passe temporaire si isTemp
        if (isTemp) {
            log.info("Mot de passe temporaire généré pour {} : {}", saved.getEmail(), rawPassword);
        }

        return UserDto.fromEntity(saved);
    }

    // ── Modifier un utilisateur ───────────────────────────────────────────────

    public UserDto updateUser(String id, UpdateUserRequest request) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Vérifier unicité email si changé
        if (!user.getEmail().equalsIgnoreCase(request.getEmail())) {
            if (userRepository.existsByEmail(request.getEmail())) {
                throw new BadRequestException("Cet email est déjà utilisé");
            }
        }

        user.setNomComplet(request.getNomComplet());
        user.setEmail(request.getEmail().toLowerCase().trim());
        user.setRole(request.getRole());

        if (request.getStatut() != null) {
            user.setStatut(request.getStatut());
        }

        User updated = userRepository.save(user);
        log.info("Utilisateur modifié par admin : {}", updated.getEmail());
        return UserDto.fromEntity(updated);
    }

    // ── Activer / Désactiver ──────────────────────────────────────────────────

    public UserDto toggleStatut(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        User.Statut newStatut = user.getStatut() == User.Statut.ACTIF
                ? User.Statut.INACTIF
                : User.Statut.ACTIF;

        user.setStatut(newStatut);
        User updated = userRepository.save(user);

        log.info("Statut utilisateur {} changé → {}", updated.getEmail(), newStatut);
        return UserDto.fromEntity(updated);
    }

    // ── Supprimer un utilisateur ──────────────────────────────────────────────

    public void deleteUser(String id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Utilisateur non trouvé"));

        // Suppression logique — désactivation plutôt que suppression physique
        // si l'utilisateur a des ventes associées
        boolean hasVentes = userRepository.hasVentes(id);
        if (hasVentes) {
            user.setStatut(User.Statut.INACTIF);
            userRepository.save(user);
            log.info("Utilisateur {} désactivé (a des ventes associées)", user.getEmail());
        } else {
            userRepository.delete(user);
            log.info("Utilisateur {} supprimé définitivement", user.getEmail());
        }
    }

    // ── Stats d'un utilisateur ────────────────────────────────────────────────

    @Transactional(readOnly = true)
    public UserStatsResponse getUserStats(String id) {
        // Délégue au UserService existant
        return userService.getUserStats(id);
    }
}