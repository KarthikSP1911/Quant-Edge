package com.quantedge.backend.cache;

import java.time.Duration;
import java.util.Optional;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Thin client over Upstash's Redis REST API. Cache failures (timeouts, Upstash errors,
 * deserialization issues) are logged and swallowed rather than thrown — per the cache-first
 * strategy in CLAUDE.md, the cache is an optimization, not a correctness dependency, so a Redis
 * outage should degrade to a cache miss (falling through to the external API / DB) rather than
 * break the request.
 */
@Component
public class RedisCacheClient {

    private static final Logger log = LoggerFactory.getLogger(RedisCacheClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public RedisCacheClient(RestClient upstashRestClient) {
        this.restClient = upstashRestClient;
    }

    public <T> Optional<T> get(String key, Class<T> type) {
        try {
            UpstashResponse response =
                    restClient.get().uri("/get/{key}", key).retrieve().body(UpstashResponse.class);
            if (response == null || response.result() == null) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(response.result().toString(), type));
        } catch (RestClientException | JsonProcessingException ex) {
            log.warn("Redis cache GET failed for key={}", key, ex);
            return Optional.empty();
        }
    }

    public void set(String key, Object value, Duration ttl) {
        try {
            String json = objectMapper.writeValueAsString(value);
            restClient
                    .post()
                    .uri(uriBuilder -> uriBuilder
                            .path("/set/{key}")
                            .queryParam("EX", ttl.toSeconds())
                            .build(key))
                    .contentType(MediaType.TEXT_PLAIN)
                    .body(json)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException | JsonProcessingException ex) {
            log.warn("Redis cache SET failed for key={}", key, ex);
        }
    }

    public void delete(String key) {
        try {
            restClient.post().uri("/del/{key}", key).retrieve().toBodilessEntity();
        } catch (RestClientException ex) {
            log.warn("Redis cache DEL failed for key={}", key, ex);
        }
    }
}
