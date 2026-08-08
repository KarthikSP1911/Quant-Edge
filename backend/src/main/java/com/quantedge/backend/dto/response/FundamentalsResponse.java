package com.quantedge.backend.dto.response;

/**
 * The fundamentals block of one stock comparison column. Every field is nullable: the comparison
 * table renders the same rows for every stock, so a metric Finnhub has no data for must come back
 * as {@code null} (rendered "—") to keep the rows aligned rather than dropping the row.
 */
public record FundamentalsResponse(Double marketCap, Double peRatio, Double fiftyTwoWeekHigh, Double fiftyTwoWeekLow) {

    public static FundamentalsResponse empty() {
        return new FundamentalsResponse(null, null, null, null);
    }
}
