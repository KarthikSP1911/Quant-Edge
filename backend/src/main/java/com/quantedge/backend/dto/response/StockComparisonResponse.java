package com.quantedge.backend.dto.response;

import java.util.List;

/**
 * A 2-3 stock comparison. {@code entries} preserves the order the symbols were requested in, and
 * every entry's candle list shares the same datetime axis (see {@link ComparisonEntryResponse}).
 */
public record StockComparisonResponse(List<ComparisonEntryResponse> entries) {}
