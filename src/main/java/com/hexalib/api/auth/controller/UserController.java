package com.hexalib.api.auth.controller;

import com.hexalib.api.auth.dto.*;
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
@Tag(name = "Users", description = "Gestion des profils utilisateurs")
@SecurityRequirement(name = "bearerAuth")
public class UserController {

    private final UserService userService;

    /**
     * Mettre à jour son propre profil
     */
    @PutMapping("/profil")
    @Operation(summary = "Modifier son profil", description = "Mettre à jour ses informations personnelles")
    public ResponseEntity<ApiResponse<UserDto>> updateProfil(
            @Valid @RequestBody UpdateProfilRequest request) {
        
        UserDto response = userService.updateProfil(request);
        return ResponseEntity.ok(ApiResponse.success("Profil mis à jour avec succès", response));
    }

    /**
     * Changer son mot de passe
     */
    @PutMapping("/change-password")
    @Operation(summary = "Changer son mot de passe", description = "Modifier son mot de passe")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        
        userService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe changé avec succès", null));
    }

    /**
     * Réinitialiser le mot de passe d'un utilisateur (Admin)
     */
    @PostMapping("/{id}/reset-password")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Réinitialiser le mot de passe", description = "Générer un nouveau mot de passe temporaire (Admin)")
    public ResponseEntity<ApiResponse<ResetPasswordResponse>> resetPassword(@PathVariable String id) {
        ResetPasswordResponse response = userService.resetPassword(id);
        return ResponseEntity.ok(ApiResponse.success("Mot de passe réinitialisé avec succès", response));
    }

    /**
     * Obtenir ses propres statistiques
     */
    @GetMapping("/my-stats")
    @Operation(summary = "Mes statistiques", description = "Récupérer ses propres statistiques de vente")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getMyStats() {
        UserStatsResponse stats = userService.getMyStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    /**
     * Obtenir les statistiques d'un vendeur (Admin)
     */
    @GetMapping("/{id}/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Statistiques vendeur", description = "Récupérer les statistiques d'un vendeur (Admin)")
    public ResponseEntity<ApiResponse<UserStatsResponse>> getUserStats(@PathVariable String id) {
        UserStatsResponse stats = userService.getUserStats(id);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}