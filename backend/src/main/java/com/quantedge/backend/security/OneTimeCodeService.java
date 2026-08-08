package com.quantedge.backend.security;

import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.entity.OAuthCode;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.repository.OAuthCodeRepository;
import com.quantedge.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * DB-backed one-time code used to hand off a successful Google OAuth2 login to the frontend.
 * Persisted (rather than kept in memory) so it survives a restart and works across multiple
 * backend instances.
 */
@Service
@RequiredArgsConstructor
public class OneTimeCodeService {

    private static final long CODE_TTL_SECONDS = 60;

    private final OAuthCodeRepository oAuthCodeRepository;
    private final UserRepository userRepository;

    @Transactional
    public String generateCode(User user) {
        String code = UUID.randomUUID().toString();
        OAuthCode entity = OAuthCode.builder()
                .code(code)
                .userId(user.getId())
                .expiresAt(Instant.now().plusSeconds(CODE_TTL_SECONDS))
                .build();
        oAuthCodeRepository.save(entity);
        return code;
    }

    @Transactional
    public User exchangeCode(String code) {
        OAuthCode entity = oAuthCodeRepository.findByCode(code).orElse(null);
        if (entity == null) {
            return null;
        }
        oAuthCodeRepository.delete(entity);

        if (entity.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        return userRepository.findById(entity.getUserId()).orElse(null);
    }
}
