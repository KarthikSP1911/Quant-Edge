package com.quantedge.backend.service;

import com.quantedge.backend.dto.auth.LoginRequest;
import com.quantedge.backend.dto.auth.OAuth2CallbackRequest;
import com.quantedge.backend.dto.auth.RefreshRequest;
import com.quantedge.backend.dto.auth.RegisterRequest;
import com.quantedge.backend.dto.auth.TokenResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.exception.EmailAlreadyInUseException;
import com.quantedge.backend.exception.InvalidCredentialsException;
import com.quantedge.backend.exception.InvalidTokenException;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.security.JwtService;
import com.quantedge.backend.security.OneTimeCodeService;
import com.quantedge.backend.security.RefreshTokenService;
import io.jsonwebtoken.JwtException;
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
    private final RefreshTokenService refreshTokenService;

    public TokenResponse register(RegisterRequest request) {
        if (userRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new EmailAlreadyInUseException("Email already in use");
        }
        var user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
        userRepository.save(user);

        return issueTokens(user);
    }

    public TokenResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        var user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() -> new InvalidCredentialsException("Invalid email or password"));

        return issueTokens(user);
    }

    public TokenResponse refresh(RefreshRequest request) {
        String refreshToken = request.getRefreshToken();
        String userEmail;
        try {
            userEmail = jwtService.extractUsernameFromRefreshToken(refreshToken);
        } catch (JwtException e) {
            throw new InvalidTokenException("Invalid refresh token");
        }

        var user = userRepository
                .findByEmail(userEmail)
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        // Validates the token is a known, unexpired, not-yet-used record and marks it consumed;
        // presenting it again is treated as theft and revokes every active session for the user.
        refreshTokenService.consume(refreshToken);

        return issueTokens(user);
    }

    public TokenResponse exchangeCode(OAuth2CallbackRequest request) {
        User user = oneTimeCodeService.exchangeCode(request.getCode());
        if (user == null) {
            throw new InvalidTokenException("Invalid or expired code");
        }
        return issueTokens(user);
    }

    public void logout(RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
    }

    private TokenResponse issueTokens(User user) {
        var accessToken = jwtService.generateAccessToken(user);
        var refreshToken = jwtService.generateRefreshToken(user);
        refreshTokenService.store(user, refreshToken);
        return TokenResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .build();
    }
}
