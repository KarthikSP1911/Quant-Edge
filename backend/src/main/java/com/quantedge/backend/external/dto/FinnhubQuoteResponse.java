package com.quantedge.backend.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response from Finnhub's {@code /quote} endpoint (real-time price snapshot). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubQuoteResponse(
        @JsonProperty("c") double currentPrice,
        @JsonProperty("h") double high,
        @JsonProperty("l") double low,
        @JsonProperty("o") double open,
        @JsonProperty("pc") double previousClose,
        @JsonProperty("t") long timestamp) {}
