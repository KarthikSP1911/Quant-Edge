package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * One realized gain/loss event: a SELL execution valued against the average-cost basis in effect
 * at that moment. {@code costBasis} is per-share, matching {@link
 * com.quantedge.backend.entity.Portfolio#getAverageCost()}.
 */
public record RealizedPnlLine(
        String symbol,
        Instant executedAt,
        int quantity,
        BigDecimal salePrice,
        BigDecimal costBasis,
        BigDecimal realizedGain) {}
