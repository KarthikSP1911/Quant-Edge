package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Company profile cache. TTL per CLAUDE.md's Data Strategy: profiles, 24 hours. */
@Component
public class ProfileCache {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "profile:";

    private final RedisCacheClient redisCacheClient;

    public ProfileCache(RedisCacheClient redisCacheClient) {
        this.redisCacheClient = redisCacheClient;
    }

    public <T> Optional<T> get(String symbol, Class<T> type) {
        return redisCacheClient.get(PREFIX + symbol, type);
    }

    public void put(String symbol, Object value) {
        redisCacheClient.set(PREFIX + symbol, value, TTL);
    }

    public void evict(String symbol) {
        redisCacheClient.delete(PREFIX + symbol);
    }
}
