package com.quantedge.backend.security;

import com.quantedge.backend.exception.RateLimitExceededException;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * In-memory brute-force guard for {@code POST /api/auth/login}, keyed by email. This is a
 * single-instance limiter backed by a local map; it does not coordinate across multiple backend
 * instances. Move this to Redis (already planned in the stack, not yet wired into the backend)
 * once the platform runs more than one instance, so limits are shared across pods.
 */
@Component
public class LoginRateLimiter {

    private static final int MAX_ATTEMPTS = 5;
    private static final Duration LOCKOUT = Duration.ofMinutes(15);

    private record Attempts(int count, Instant lockedUntil) {}

    private final Map<String, Attempts> attemptsByEmail = new ConcurrentHashMap<>();

    public void checkAllowed(String email) {
        Attempts attempts = attemptsByEmail.get(normalize(email));
        if (attempts != null && attempts.lockedUntil() != null && Instant.now().isBefore(attempts.lockedUntil())) {
            throw new RateLimitExceededException("Too many failed login attempts. Try again later.");
        }
    }

    public void recordFailure(String email) {
        attemptsByEmail.compute(normalize(email), (key, current) -> {
            int count = (current == null ? 0 : current.count()) + 1;
            Instant lockedUntil = count >= MAX_ATTEMPTS ? Instant.now().plus(LOCKOUT) : null;
            return new Attempts(count, lockedUntil);
        });
    }

    public void recordSuccess(String email) {
        attemptsByEmail.remove(normalize(email));
    }

    private String normalize(String email) {
        return email.toLowerCase(Locale.ROOT);
    }
}
