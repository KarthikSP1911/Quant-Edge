package com.quantedge.backend.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/** Response from Finnhub's {@code /stock/profile2} endpoint (company reference data). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubCompanyProfileResponse(
        @JsonProperty("name") String name,
        @JsonProperty("ticker") String ticker,
        @JsonProperty("exchange") String exchange,
        @JsonProperty("finnhubIndustry") String industry,
        @JsonProperty("logo") String logoUrl,
        @JsonProperty("marketCapitalization") double marketCapitalization,
        @JsonProperty("currency") String currency,
        @JsonProperty("weburl") String webUrl) {}
