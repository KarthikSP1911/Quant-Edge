package com.quantedge.backend.controller;

import com.quantedge.backend.dto.auth.AccessTokenResponse;
import com.quantedge.backend.dto.auth.AuthUserResponse;
import com.quantedge.backend.dto.auth.LoginRequest;
import com.quantedge.backend.dto.auth.OAuth2CallbackRequest;
import com.quantedge.backend.dto.auth.RegisterRequest;
import com.quantedge.backend.dto.auth.TokenResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.AuthService;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private static final String REFRESH_COOKIE_NAME = "refreshToken";
    private static final String REFRESH_COOKIE_PATH = "/api/auth";

    private final AuthService authService;

    @Value("${app.cookie-secure:true}")
    private boolean cookieSecure;

    @Value("${jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @PostMapping("/register")
    public ResponseEntity<AccessTokenResponse> register(
            @Valid @RequestBody RegisterRequest request, HttpServletResponse response) {
        return respondWithTokens(authService.register(request), response);
    }

    @PostMapping("/login")
    public ResponseEntity<AccessTokenResponse> login(
            @Valid @RequestBody LoginRequest request, HttpServletResponse response) {
        return respondWithTokens(authService.login(request), response);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AccessTokenResponse> refresh(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        if (refreshToken == null) {
            throw new com.quantedge.backend.exception.InvalidTokenException("Missing refresh token");
        }
        return respondWithTokens(authService.refresh(refreshToken), response);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @CookieValue(name = REFRESH_COOKIE_NAME, required = false) String refreshToken,
            HttpServletResponse response) {
        authService.logout(refreshToken);
        clearRefreshCookie(response);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/oauth2/callback")
    public ResponseEntity<AccessTokenResponse> oauth2Callback(
            @Valid @RequestBody OAuth2CallbackRequest request, HttpServletResponse response) {
        return respondWithTokens(authService.exchangeCode(request), response);
    }

    @GetMapping("/me")
    public ResponseEntity<AuthUserResponse> getMe(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(AuthUserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .role(user.getRole())
                .build());
    }

    private ResponseEntity<AccessTokenResponse> respondWithTokens(TokenResponse tokens, HttpServletResponse response) {
        setRefreshCookie(response, tokens.getRefreshToken());
        return ResponseEntity.ok(AccessTokenResponse.builder()
                .accessToken(tokens.getAccessToken())
                .build());
    }

    private void setRefreshCookie(HttpServletResponse response, String refreshToken) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(refreshExpirationMs / 1000)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }

    private void clearRefreshCookie(HttpServletResponse response) {
        ResponseCookie cookie = ResponseCookie.from(REFRESH_COOKIE_NAME, "")
                .httpOnly(true)
                .secure(cookieSecure)
                .sameSite("Lax")
                .path(REFRESH_COOKIE_PATH)
                .maxAge(0)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
