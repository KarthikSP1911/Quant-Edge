package com.quantedge.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.dto.auth.LoginRequest;
import com.quantedge.backend.dto.auth.RegisterRequest;
import com.quantedge.backend.dto.auth.TokenResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.exception.EmailAlreadyInUseException;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.security.JwtService;
import com.quantedge.backend.security.OneTimeCodeService;
import com.quantedge.backend.security.RefreshTokenService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private OneTimeCodeService oneTimeCodeService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private com.quantedge.backend.security.LoginRateLimiter loginRateLimiter;

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
                .id(UUID.randomUUID())
                .email("test@example.com")
                .passwordHash("hashed")
                .name("Test User")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build();
    }

    @Test
    void register_Success() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.empty());
        when(passwordEncoder.encode("password")).thenReturn("hashed");
        when(jwtService.generateAccessToken(any(User.class))).thenReturn("access");
        when(jwtService.generateRefreshToken(any(User.class))).thenReturn("refresh");

        TokenResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(userRepository).save(any(User.class));
    }

    @Test
    void register_EmailTaken() {
        RegisterRequest request = new RegisterRequest("Test User", "test@example.com", "password");
        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(EmailAlreadyInUseException.class, () -> authService.register(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmailIgnoreCase("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("access");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh");

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(authenticationManager).authenticate(any());
        verify(loginRateLimiter).checkAllowed("test@example.com");
        verify(loginRateLimiter).recordSuccess("test@example.com");
    }

    @Test
    void login_LockedOutRejectsWithoutCallingAuthenticationManager() {
        LoginRequest request = new LoginRequest("test@example.com", "password");
        doThrow(new com.quantedge.backend.exception.RateLimitExceededException("Too many attempts"))
                .when(loginRateLimiter)
                .checkAllowed("test@example.com");

        assertThrows(
                com.quantedge.backend.exception.RateLimitExceededException.class, () -> authService.login(request));
        verify(authenticationManager, never()).authenticate(any());
    }

    @Test
    void login_FailedAuthenticationRecordsFailure() {
        LoginRequest request = new LoginRequest("test@example.com", "wrong-password");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new org.springframework.security.authentication.BadCredentialsException("bad creds"));

        assertThrows(
                org.springframework.security.authentication.BadCredentialsException.class,
                () -> authService.login(request));
        verify(loginRateLimiter).recordFailure("test@example.com");
    }

    @Test
    void refresh_Success() {
        when(jwtService.extractUsernameFromRefreshToken("refresh")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh");

        TokenResponse response = authService.refresh("refresh");

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
        verify(refreshTokenService).consume("refresh");
        verify(refreshTokenService).store(testUser, "new-refresh");
    }

    @Test
    void refresh_RejectsReusedOrUnknownToken() {
        when(jwtService.extractUsernameFromRefreshToken("refresh")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        doThrow(new com.quantedge.backend.exception.InvalidTokenException("Refresh token reuse detected"))
                .when(refreshTokenService)
                .consume("refresh");

        assertThrows(com.quantedge.backend.exception.InvalidTokenException.class, () -> authService.refresh("refresh"));
    }

    @Test
    void logout_RevokesToken() {
        authService.logout("refresh");
        verify(refreshTokenService).revoke("refresh");
    }

    @Test
    void logout_NullTokenIsNoOp() {
        authService.logout(null);
        verify(refreshTokenService, never()).revoke(any());
    }
}
