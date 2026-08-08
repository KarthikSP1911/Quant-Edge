package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Computed technical indicator cache. TTL per CLAUDE.md's Data Strategy: indicators, 24 hours. */
@Component
public class IndicatorCache {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "indicator:";

    private final RedisCacheClient redisCacheClient;

    public IndicatorCache(RedisCacheClient redisCacheClient) {
        this.redisCacheClient = redisCacheClient;
    }

    public <T> Optional<T> get(String symbol, String indicator, Class<T> type) {
        return redisCacheClient.get(key(symbol, indicator), type);
    }

    public void put(String symbol, String indicator, Object value) {
        redisCacheClient.set(key(symbol, indicator), value, TTL);
    }

    public void evict(String symbol, String indicator) {
        redisCacheClient.delete(key(symbol, indicator));
    }

    private String key(String symbol, String indicator) {
        return PREFIX + symbol + ":" + indicator;
    }
}
