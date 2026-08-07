package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PortfolioSummaryResponse(
        BigDecimal cashBalance,
        List<PortfolioPositionResponse> positions,
        BigDecimal totalMarketValue,
        BigDecimal totalAccountValue) {}
