package com.quantedge.backend.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Response from Finnhub's {@code /stock/metric?metric=all} endpoint (basic financials).
 *
 * <p>Only the handful of metrics the stock comparison needs are mapped; Finnhub returns well over a
 * hundred. Every field is boxed rather than primitive because Finnhub omits metrics it has no data
 * for (and gates some behind paid tiers) — a missing metric must surface as {@code null} ("—" in the
 * UI), not as a misleading 0.0.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubBasicFinancialsResponse(@JsonProperty("metric") Metric metric) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Metric(
            @JsonProperty("marketCapitalization") Double marketCapitalization,
            @JsonProperty("peTTM") Double peRatio,
            @JsonProperty("52WeekHigh") Double fiftyTwoWeekHigh,
            @JsonProperty("52WeekLow") Double fiftyTwoWeekLow) {}
}
