package com.hexalib.api.auth.service;

import com.hexalib.api.auth.dto.LoginRequest;
import com.hexalib.api.auth.dto.LoginResponse;
import com.hexalib.api.auth.dto.RegisterRequest;
import com.hexalib.api.auth.dto.UserDto;
import com.hexalib.api.auth.entity.User;
import com.hexalib.api.auth.repository.UserRepository;
import com.hexalib.api.auth.security.JwtTokenProvider;
import com.hexalib.api.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider tokenProvider;

    @Transactional
    public UserDto register(RegisterRequest request) {
        // Vérifier si l'email existe déjà
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new BadRequestException("Cet email est déjà utilisé");
        }

        // Créer l'utilisateur
        User user = new User();
        user.setNomComplet(request.getNomComplet());
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setRole(request.getRole());
        user.setStatut(User.Statut.ACTIF);
        user.setDateCreation(LocalDateTime.now());

        User savedUser = userRepository.save(user);

        return UserDto.fromEntity(savedUser);
    }

    @Transactional
    public LoginResponse login(LoginRequest request) {
        // Authentifier l'utilisateur
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        request.getEmail(),
                        request.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // Générer le token JWT
        String token = tokenProvider.generateToken(authentication);

        // Récupérer l'utilisateur et mettre à jour la dernière connexion
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        user.setDerniereConnexion(LocalDateTime.now());
        userRepository.save(user);

        UserDto userDto = UserDto.fromEntity(user);

        return new LoginResponse(token, userDto);
    }

    public UserDto getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new BadRequestException("Utilisateur non trouvé"));

        return UserDto.fromEntity(user);
    }
}