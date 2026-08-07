package com.quantedge.backend.service;

import com.quantedge.backend.dto.auth.LoginRequest;
import com.quantedge.backend.dto.auth.OAuth2CallbackRequest;
import com.quantedge.backend.dto.auth.RefreshRequest;
import com.quantedge.backend.dto.auth.RegisterRequest;
import com.quantedge.backend.dto.auth.TokenResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.security.JwtService;
import com.quantedge.backend.security.OneTimeCodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final OneTimeCodeService oneTimeCodeService;

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new IllegalArgumentException("Email already in use");
        }
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new IllegalArgumentException("Invalid email or password"));

        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }

    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail = jwtService.extractUsernameFromRefreshToken(refreshToken);

        if (userEmail != null) {
            var user = userRepository
                    .findByEmail(userEmail)
                    .orElseThrow(() -> new IllegalArgumentException("User not found"));
            if (jwtService.isRefreshTokenValid(refreshToken, user)) {
                var accessToken = jwtService.generateAccessToken(user);
                var newRefreshToken = jwtService.generateRefreshToken(user);
                return TokenResponse.builder()
                        .accessToken(accessToken)
                        .refreshToken(newRefreshToken)
                        .build();
            }
        }
        throw new IllegalArgumentException("Invalid refresh token");
    }

    public TokenResponse exchangeCode(OAuth2CallbackRequest request) {
        User user = oneTimeCodeService.exchangeCode(request.getCode());
        if (user == null) {
            throw new IllegalArgumentException("Invalid or expired code");
        }
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
