package com.quantedge.backend.dto.response;

import java.util.List;

/**
 * One column of a stock comparison: the company, its live quote, its fundamentals, and its candles
 * on the comparison's shared time axis.
 *
 * <p>{@code candles} is aligned across every entry of a {@link StockComparisonResponse} — same
 * length, same datetimes, same order — so the frontend can overlay the series without re-aligning
 * them.
 */
public record ComparisonEntryResponse(
        CompanyResponse company,
        QuoteResponse quote,
        FundamentalsResponse fundamentals,
        List<CandleResponse> candles) {}
