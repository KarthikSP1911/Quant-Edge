package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Basic-financials cache (market cap, P/E, 52-week high/low). These are reference-data style
 * metrics that move at most once a day, so they share the 24-hour TTL CLAUDE.md's Data Strategy
 * assigns to profiles — kept in its own key space rather than folded into {@link ProfileCache} so
 * the two can be evicted independently.
 */
@Component
public class FundamentalsCache {

    private static final Duration TTL = Duration.ofHours(24);
    private static final String PREFIX = "fundamentals:";

    private final RedisCacheClient redisCacheClient;

    public FundamentalsCache(RedisCacheClient redisCacheClient) {
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
