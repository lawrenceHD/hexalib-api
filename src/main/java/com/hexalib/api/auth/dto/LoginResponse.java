package com.hexalib.api.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {

    private String accessToken;
    private String refreshToken;
    private String type = "Bearer";
    private long   expiresIn;   // ms
    private UserDto user;

    // Constructeur de compatibilité pour l'existant
    public LoginResponse(String accessToken, UserDto user) {
        this.accessToken = accessToken;
        this.user        = user;
    }
}