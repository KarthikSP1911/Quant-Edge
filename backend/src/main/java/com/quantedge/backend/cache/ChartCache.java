package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * OHLCV chart series cache. TTL per CLAUDE.md's Data Strategy: charts, 15-60 minutes — callers
 * pick a TTL within that range based on the chart's interval (e.g. shorter for intraday, longer
 * for daily+).
 */
@Component
public class ChartCache {

    private static final String PREFIX = "chart:";

    private final RedisCacheClient redisCacheClient;

    public ChartCache(RedisCacheClient redisCacheClient) {
        this.redisCacheClient = redisCacheClient;
    }

    public <T> Optional<T> get(String symbol, String interval, Class<T> type) {
        return redisCacheClient.get(key(symbol, interval), type);
    }

    public void put(String symbol, String interval, Object value, Duration ttl) {
        redisCacheClient.set(key(symbol, interval), value, ttl);
    }

    public void evict(String symbol, String interval) {
        redisCacheClient.delete(key(symbol, interval));
    }

    private String key(String symbol, String interval) {
        return PREFIX + symbol + ":" + interval;
    }
}
