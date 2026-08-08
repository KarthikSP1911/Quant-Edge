package com.quantedge.backend.security;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class OAuth2SuccessHandlerTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private OneTimeCodeService oneTimeCodeService;

    @InjectMocks
    private OAuth2SuccessHandler handler;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(handler, "frontendUrl", "http://localhost:3000");
    }

    @Test
    void preRegisteredUnverifiedLocalAccountHasPasswordInvalidatedOnGoogleLink() throws Exception {
        User attackerPlantedAccount = User.builder()
                .id(UUID.randomUUID())
                .email("victim@example.com")
                .name("Victim")
                .passwordHash("attacker-controlled-hash")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(false)
                .build();

        OAuth2User oAuth2User = new DefaultOAuth2User(
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER")),
                java.util.Map.of("email", "victim@example.com", "name", "Victim", "sub", "google-sub-id"),
                "email");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(userRepository.findByEmail("victim@example.com")).thenReturn(Optional.of(attackerPlantedAccount));
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(oneTimeCodeService.generateCode(any(User.class))).thenReturn("one-time-code");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.onAuthenticationSuccess(request, response, authentication);

        ArgumentCaptor<User> savedUser = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(savedUser.capture());
        assertNull(savedUser.getValue().getPasswordHash(), "attacker-set password must be invalidated");
        assertTrue(savedUser.getValue().isEmailVerified());
        assertEquals("google-sub-id", savedUser.getValue().getProviderId());
    }

    @Test
    void alreadyVerifiedAccountIsLeftUntouched() throws Exception {
        User verifiedUser = User.builder()
                .id(UUID.randomUUID())
                .email("owner@example.com")
                .name("Owner")
                .passwordHash("owners-own-hash")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .emailVerified(true)
                .build();

        OAuth2User oAuth2User = new DefaultOAuth2User(
                java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("USER")),
                java.util.Map.of("email", "owner@example.com", "name", "Owner", "sub", "google-sub-id"),
                "email");
        Authentication authentication = mock(Authentication.class);
        when(authentication.getPrincipal()).thenReturn(oAuth2User);

        when(userRepository.findByEmail("owner@example.com")).thenReturn(Optional.of(verifiedUser));
        when(oneTimeCodeService.generateCode(any(User.class))).thenReturn("one-time-code");

        HttpServletRequest request = mock(HttpServletRequest.class);
        HttpServletResponse response = mock(HttpServletResponse.class);

        handler.onAuthenticationSuccess(request, response, authentication);

        verify(userRepository, never()).save(any(User.class));
        assertEquals("owners-own-hash", verifiedUser.getPasswordHash());
    }
}
