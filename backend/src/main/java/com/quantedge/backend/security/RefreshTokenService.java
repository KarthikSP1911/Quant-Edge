package com.quantedge.backend.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.HexFormat;

import com.quantedge.backend.entity.RefreshToken;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.InvalidTokenException;
import com.quantedge.backend.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists a hash of every issued refresh token so it can be single-use (rotated) and revoked.
 * Presenting an already-revoked token is treated as evidence of token theft and revokes every
 * active token for that user, forcing re-authentication everywhere.
 */
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtService jwtService;

    @Transactional
    public void store(User user, String rawRefreshToken) {
        RefreshToken record = RefreshToken.builder()
                .userId(user.getId())
                .tokenHash(hash(rawRefreshToken))
                .expiresAt(jwtService
                        .extractRefreshTokenExpiration(rawRefreshToken)
                        .toInstant())
                .revoked(false)
                .build();
        refreshTokenRepository.save(record);
    }

    /** Validates and single-use-consumes a refresh token, returning the owning user id. */
    @Transactional
    public java.util.UUID consume(String rawRefreshToken) {
        RefreshToken record = refreshTokenRepository
                .findByTokenHash(hash(rawRefreshToken))
                .orElseThrow(() -> new InvalidTokenException("Invalid refresh token"));

        if (record.isRevoked()) {
            refreshTokenRepository.revokeAllForUser(record.getUserId());
            throw new InvalidTokenException("Refresh token reuse detected; all sessions revoked");
        }
        if (record.getExpiresAt().isBefore(Instant.now())) {
            throw new InvalidTokenException("Refresh token expired");
        }

        record.setRevoked(true);
        refreshTokenRepository.save(record);
        return record.getUserId();
    }

    @Transactional
    public void revoke(String rawRefreshToken) {
        refreshTokenRepository.findByTokenHash(hash(rawRefreshToken)).ifPresent(record -> {
            record.setRevoked(true);
            refreshTokenRepository.save(record);
        });
    }

    private String hash(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(token.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
