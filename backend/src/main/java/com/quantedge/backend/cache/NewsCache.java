package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;

import org.springframework.stereotype.Component;

/** Company news cache. TTL per CLAUDE.md's Data Strategy: news, 1 hour. */
@Component
public class NewsCache {

    private static final Duration TTL = Duration.ofHours(1);
    private static final String PREFIX = "news:";

    private final RedisCacheClient redisCacheClient;

    public NewsCache(RedisCacheClient redisCacheClient) {
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
