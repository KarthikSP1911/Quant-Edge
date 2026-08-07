package com.quantedge.backend.external.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

/** Response from Twelve Data's {@code /time_series} endpoint (OHLCV candles). */
@JsonIgnoreProperties(ignoreUnknown = true)
public record TwelveDataTimeSeriesResponse(
        @JsonProperty("meta") Meta meta,
        @JsonProperty("values") List<Candle> values,
        @JsonProperty("status") String status,
        @JsonProperty("message") String message) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Meta(
            @JsonProperty("symbol") String symbol,
            @JsonProperty("interval") String interval,
            @JsonProperty("exchange") String exchange,
            @JsonProperty("currency") String currency) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Candle(
            @JsonProperty("datetime") String datetime,
            @JsonProperty("open") String open,
            @JsonProperty("high") String high,
            @JsonProperty("low") String low,
            @JsonProperty("close") String close,
            @JsonProperty("volume") String volume) {}
}
