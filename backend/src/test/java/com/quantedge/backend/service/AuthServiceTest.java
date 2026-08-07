package com.quantedge.backend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.quantedge.backend.dto.auth.LoginRequest;
import com.quantedge.backend.dto.auth.RefreshRequest;
import com.quantedge.backend.dto.auth.RegisterRequest;
import com.quantedge.backend.dto.auth.TokenResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.exception.EmailAlreadyInUseException;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.security.JwtService;
import com.quantedge.backend.security.OneTimeCodeService;
import java.util.Optional;
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

    @InjectMocks
    private AuthService authService;

    private User testUser;

    @BeforeEach
    void setUp() {
        testUser = User.builder()
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

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.empty());
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
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));

        assertThrows(EmailAlreadyInUseException.class, () -> authService.register(request));
    }

    @Test
    void login_Success() {
        LoginRequest request = new LoginRequest("test@example.com", "password");

        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.generateAccessToken(testUser)).thenReturn("access");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("refresh");

        TokenResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("access", response.getAccessToken());
        assertEquals("refresh", response.getRefreshToken());
        verify(authenticationManager).authenticate(any());
    }

    @Test
    void refresh_Success() {
        RefreshRequest request = new RefreshRequest("refresh");

        when(jwtService.extractUsernameFromRefreshToken("refresh")).thenReturn("test@example.com");
        when(userRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testUser));
        when(jwtService.isRefreshTokenValid("refresh", testUser)).thenReturn(true);
        when(jwtService.generateAccessToken(testUser)).thenReturn("new-access");
        when(jwtService.generateRefreshToken(testUser)).thenReturn("new-refresh");

        TokenResponse response = authService.refresh(request);

        assertNotNull(response);
        assertEquals("new-access", response.getAccessToken());
        assertEquals("new-refresh", response.getRefreshToken());
    }
}
