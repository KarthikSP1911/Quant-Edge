package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Real-time quote cache. TTL per CLAUDE.md's Data Strategy: prices, 15 minutes. */
@Component
public class PriceCache {

    private static final Duration TTL = Duration.ofMinutes(15);
    private static final String PREFIX = "price:";

    private final RedisCacheClient redisCacheClient;

    public PriceCache(RedisCacheClient redisCacheClient) {
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
