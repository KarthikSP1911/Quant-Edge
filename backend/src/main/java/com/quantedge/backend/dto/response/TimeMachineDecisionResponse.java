package com.quantedge.backend.dto.response;

import java.math.BigDecimal;

public record TimeMachineDecisionResponse(
        String symbol,
        int quantity,
        BigDecimal buyPrice,
        BigDecimal sellPrice,
        String executedAt,
        BigDecimal realizedGainPercent) {}
