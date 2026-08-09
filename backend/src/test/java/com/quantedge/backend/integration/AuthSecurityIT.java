package com.quantedge.backend.integration;

import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Date;
import javax.crypto.SecretKey;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantedge.backend.dto.auth.LoginRequest;
import com.quantedge.backend.dto.auth.RegisterRequest;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockCookie;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Security smoke tests against real Postgres: unauthenticated/expired/invalid access tokens are
 * rejected, and refresh tokens are single-use — a second presentation of an already-rotated
 * refresh token is treated as reuse and revokes the session rather than being honored.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class AuthSecurityIT {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void overrideProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.datasource.driver-class-name", () -> "org.postgresql.Driver");
        registry.add("spring.jpa.hibernate.ddl-auto", () -> "validate");
        registry.add("spring.flyway.enabled", () -> "true");
    }

    @Autowired
    private WebApplicationContext context;

    @Value("${jwt.access-secret}")
    private String accessSecret;

    private MockMvc mockMvc;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();
    }

    @Test
    void unauthenticatedRequestToProtectedEndpointReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void invalidAccessTokenReturns401() throws Exception {
        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void expiredAccessTokenReturns401() throws Exception {
        String expiredToken = buildExpiredAccessToken("expired@example.com");

        mockMvc.perform(get("/api/auth/me").header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void refreshTokenRotationRevokesOldTokenOnReuse() throws Exception {
        String email = "rotation@example.com";
        register("Rotation User", email, "password123");

        MockCookie firstRefreshCookie = login(email, "password123");

        // First refresh: rotates the token and issues a new one.
        var firstRefreshResult = mockMvc.perform(post("/api/auth/refresh").cookie(firstRefreshCookie))
                .andExpect(status().isOk())
                .andReturn();
        MockCookie secondRefreshCookie =
                (MockCookie) firstRefreshResult.getResponse().getCookie("refreshToken");
        Assertions.assertThat(secondRefreshCookie).isNotNull();

        // Reusing the already-rotated (now revoked) first refresh token must be rejected.
        mockMvc.perform(post("/api/auth/refresh").cookie(firstRefreshCookie)).andExpect(status().isUnauthorized());

        // Reuse detection revokes every session for the user, so even the second (legitimately
        // rotated) token is no longer honored.
        mockMvc.perform(post("/api/auth/refresh").cookie(secondRefreshCookie)).andExpect(status().isUnauthorized());
    }

    @Test
    void missingRefreshTokenReturns401OnRefresh() throws Exception {
        mockMvc.perform(post("/api/auth/refresh")).andExpect(status().isUnauthorized());
    }

    private void register(String name, String email, String password) throws Exception {
        RegisterRequest request = new RegisterRequest(name, email, password);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
    }

    private MockCookie login(String email, String password) throws Exception {
        LoginRequest request = new LoginRequest(email, password);
        var result = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andReturn();
        MockCookie refreshCookie = (MockCookie) result.getResponse().getCookie("refreshToken");
        Assertions.assertThat(refreshCookie).isNotNull();
        return refreshCookie;
    }

    private String buildExpiredAccessToken(String subject) {
        SecretKey key = Keys.hmacShaKeyFor(Decoders.BASE64.decode(accessSecret));
        long past = System.currentTimeMillis() - 60_000;
        return Jwts.builder()
                .subject(subject)
                .issuedAt(new Date(past - 60_000))
                .expiration(new Date(past))
                .signWith(key)
                .compact();
    }
}
