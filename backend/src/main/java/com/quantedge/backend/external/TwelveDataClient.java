package com.quantedge.backend.external;

import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for Twelve Data (https://twelvedata.com) OHLCV candle history, used for charting and
 * server-side indicator computation (SMA, EMA, RSI, MACD, etc.).
 *
 * <p>Free tier is capped at 800 requests/day — callers are responsible for staying under that
 * limit (e.g. via the Redis cache layer / price-sync scheduler); this client does not rate-limit
 * itself.
 */
@Component
public class TwelveDataClient {

    private static final Logger log = LoggerFactory.getLogger(TwelveDataClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public TwelveDataClient(RestClient twelveDataRestClient, @Value("${twelvedata.api-key}") String apiKey) {
        this.restClient = twelveDataRestClient;
        this.apiKey = apiKey;
    }

    public TwelveDataTimeSeriesResponse getTimeSeries(String symbol, String interval, int outputSize) {
        try {
            TwelveDataTimeSeriesResponse response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/time_series")
                            .queryParam("symbol", symbol)
                            .queryParam("interval", interval)
                            .queryParam("outputsize", outputSize)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .body(TwelveDataTimeSeriesResponse.class);

            if (response != null && "error".equals(response.status())) {
                throw new ExternalApiException(
                        "Twelve Data returned an error for " + symbol + ": " + response.message());
            }
            return response;
        } catch (RestClientException ex) {
            log.warn("Twelve Data time_series request failed for symbol={}", symbol, ex);
            throw new ExternalApiException("Failed to fetch time series for " + symbol + " from Twelve Data", ex);
        }
    }
}
