package com.quantedge.backend.external;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.dto.FinnhubBasicFinancialsResponse;
import com.quantedge.backend.external.dto.FinnhubCompanyProfileResponse;
import com.quantedge.backend.external.dto.FinnhubNewsResponse;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

/**
 * Client for Finnhub (https://finnhub.io) real-time quotes, company profiles and basic financials.
 *
 * <p>Free tier is capped at 60 requests/minute — callers are responsible for staying under that
 * limit (e.g. via the Redis cache layer / price-sync scheduler); this client does not rate-limit
 * itself.
 */
@Component
public class FinnhubClient {

    private static final Logger log = LoggerFactory.getLogger(FinnhubClient.class);

    private final RestClient restClient;
    private final String apiKey;

    public FinnhubClient(RestClient finnhubRestClient, @Value("${finnhub.api-key}") String apiKey) {
        this.restClient = finnhubRestClient;
        this.apiKey = apiKey;
    }

    public FinnhubQuoteResponse getQuote(String symbol) {
        try {
            return restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/quote")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubQuoteResponse.class);
        } catch (RestClientException ex) {
            log.warn("Finnhub quote request failed for symbol={}", symbol, ex);
            throw new ExternalApiException("Failed to fetch quote for " + symbol + " from Finnhub", ex);
        }
    }

    /**
     * Fetches Finnhub's "basic financials" bundle — market cap, trailing P/E and 52-week high/low.
     * Backs the stock comparison's fundamentals rows; callers cache the result (24h) so a
     * comparison costs at most one call per symbol per day.
     */
    public FinnhubBasicFinancialsResponse getBasicFinancials(String symbol) {
        try {
            return restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/metric")
                            .queryParam("symbol", symbol)
                            .queryParam("metric", "all")
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubBasicFinancialsResponse.class);
        } catch (RestClientException ex) {
            log.warn("Finnhub basic financials request failed for symbol={}", symbol, ex);
            throw new ExternalApiException("Failed to fetch basic financials for " + symbol + " from Finnhub", ex);
        }
    }

    public FinnhubCompanyProfileResponse getCompanyProfile(String symbol) {
        try {
            return restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/stock/profile2")
                            .queryParam("symbol", symbol)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubCompanyProfileResponse.class);
        } catch (RestClientException ex) {
            log.warn("Finnhub company profile request failed for symbol={}", symbol, ex);
            throw new ExternalApiException("Failed to fetch company profile for " + symbol + " from Finnhub", ex);
        }
    }

    public List<FinnhubNewsResponse> getCompanyNews(String symbol, LocalDate from, LocalDate to) {
        String fromStr = from.format(DateTimeFormatter.ISO_LOCAL_DATE);
        String toStr = to.format(DateTimeFormatter.ISO_LOCAL_DATE);
        try {
            FinnhubNewsResponse[] response = restClient
                    .get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/company-news")
                            .queryParam("symbol", symbol)
                            .queryParam("from", fromStr)
                            .queryParam("to", toStr)
                            .queryParam("token", apiKey)
                            .build())
                    .retrieve()
                    .body(FinnhubNewsResponse[].class);
            return response != null ? List.of(response) : List.of();
        } catch (RestClientException ex) {
            log.warn("Finnhub company news request failed for symbol={}", symbol, ex);
            throw new ExternalApiException("Failed to fetch company news for " + symbol + " from Finnhub", ex);
        }
    }
}
