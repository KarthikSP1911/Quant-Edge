package com.quantedge.backend.config;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * The app talks to Upstash's Redis REST API (see {@link com.quantedge.backend.cache.RedisCacheClient}),
 * not spring-data-redis, so there's no autoconfigured {@code RedisHealthIndicator} to rely on -
 * this pings Upstash's {@code /ping} endpoint over the same {@code upstashRestClient} bean instead.
 */
@Component
public class RedisHealthIndicator implements HealthIndicator {

    private final RestClient upstashRestClient;

    public RedisHealthIndicator(RestClient upstashRestClient) {
        this.upstashRestClient = upstashRestClient;
    }

    @Override
    public Health health() {
        try {
            String result = upstashRestClient.get().uri("/ping").retrieve().body(String.class);
            return Health.up().withDetail("response", result).build();
        } catch (RestClientException ex) {
            return Health.down(ex).build();
        }
    }
}
