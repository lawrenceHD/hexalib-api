package com.hexalib.api.auth.controller;

import com.hexalib.api.auth.dto.*;
import com.hexalib.api.auth.model.User;
import com.hexalib.api.auth.service.AdminUserService;
import com.hexalib.api.auth.service.UserService;
import com.hexalib.api.common.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "Users", description = "Gestion des utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService      userService;
    private final AdminUserService adminUserService;

    // ══════════════════════════════════════════════════════════════════════════
    // ADMIN — Gestion complète des utilisateurs
    // ══════════════════════════════════════════════════════════════════════════

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Liste paginée des utilisateurs")
    public ResponseEntity<ApiResponse<UserPageResponse>> getUsers(
            @RequestParam(defaultValue = "0")    int page,
            @RequestParam(defaultValue = "20")   int size,
            @RequestParam(required = false)      String search,
            @RequestParam(required = false)      User.Role role,
            @RequestParam(required = false)      User.Statut statut) {

        UserPageResponse result = adminUserService.getUsers(page, size, search, role, statut);
        return ResponseEntity.ok(ApiResponse.success(result));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Obtenir un utilisateur par ID")
    public ResponseEntity<ApiResponse<UserDto>> getUser(@PathVariable String id) {
        UserDto user = adminUserService.getUser(id);
        return ResponseEntity.ok(ApiResponse.success(user));
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Créer un utilisateur")
    public ResponseEntity<ApiResponse<UserDto>> createUser(
            @Valid @RequestBody CreateUserRequest request) {
        UserDto user = adminUserService.createUser(request);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur créé avec succès", user));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Modifier un utilisateur")
    public ResponseEntity<ApiResponse<UserDto>> updateUser(
            @PathVariable String id,
            @Valid @RequestBody UpdateUserRequest request) {
        UserDto user = adminUserService.updateUser(id, request);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur mis à jour avec succès", user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Supprimer un utilisateur (logique si ventes associées)")
    public ResponseEntity<ApiResponse<Void>> deleteUser(@PathVariable String id) {
        adminUserService.deleteUser(id);
        return ResponseEntity.ok(ApiResponse.success("Utilisateur supprimé avec succès", null));
    }

    @PatchMapping("/{id}/toggle-statut")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Activer ou désactiver un utilisateur")
    public ResponseEntity<ApiResponse<UserDto>> toggleStatut(@PathVariable String id) {
        UserDto user = adminUserService.toggleStatut(id);
        String msg = user.getStatut().name().equals("ACTIF") ? "Utilisateur activé" : "Utilisateur désactivé";
        return ResponseEntity.ok(ApiResponse.success(msg, user));
    }

    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réinitialiser le mot de passe d'un utilisateur")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable String id) {
        ResetPasswordResponse response = userService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe réinitialisé avec succès", response));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques d'un vendeur")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(@PathVariable String id) {
        UserStatsResponse stats = adminUserService.getUserStats(id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ══════════════════════════════════════════════════════════════════════════
    // UTILISATEUR CONNECTÉ — Profil personnel
    // ══════════════════════════════════════════════════════════════════════════

    @PutMapping("/profil")
    @Operation(summary = "Modifier son propre profil")
    public ResponseEntity<ApiResponse<UserDto>> updateProfil(
            @Valid @RequestBody UpdateProfilRequest request) {
        UserDto user = userService.updateProfil(request);
        return ResponseEntity.ok(ApiResponse.success("Profil mis à jour avec succès", user));
    }

    @PutMapping("/change-password")
    @Operation(summary = "Changer son mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès", null));
    }

    @GetMapping("/my-stats")
    @Operation(summary = "Mes statistiques personnelles")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getMyStats() {
        UserStatsResponse stats = userService.getMyStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}