package com.quantedge.backend.external;

import com.quantedge.backend.exception.ExternalApiException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

@Component
public class AlphaVantageClient {

    private static final Logger log = LoggerFactory.getLogger(AlphaVantageClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public AlphaVantageClient(RestClient alphaVantageRestClient, @Value("${alphavantage.api-key}") String apiKey) {
        this.restClient = alphaVantageRestClient;
        this.apiKey = apiKey;
    }

    public String getIndicator(String symbol, String function) {
        try {
            return restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/query")
                            .queryParam("function", function)
                            .queryParam("symbol", symbol)
                            .queryParam("interval", "daily")
                            .queryParam("time_period", "14")
                            .queryParam("series_type", "close")
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(String.class);
        } catch (RestClientException ex) {
            log.warn("Alpha Vantage request failed for symbol={} function={}", symbol, function, ex);
            throw new ExternalApiException(
                    "Failed to fetch " + function + " for " + symbol + " from Alpha Vantage", ex);
        }
    }
}
