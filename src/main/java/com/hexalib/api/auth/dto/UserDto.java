package com.hexalib.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

import com.hexalib.api.auth.model.User;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserDto {
    private String id;
    private String nomComplet;
    private String email;
    private User.Role role;
    private User.Statut statut;
    private LocalDateTime dateCreation;
    private LocalDateTime derniereConnexion;

    public static UserDto fromEntity(User user) {
        return new UserDto(
                user.getId(),
                user.getNomComplet(),
                user.getEmail(),
                user.getRole(),
                user.getStatut(),
                user.getDateCreation(),
                user.getDerniereConnexion()
        );
    }
}