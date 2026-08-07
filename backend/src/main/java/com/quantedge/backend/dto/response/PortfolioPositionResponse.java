package com.quantedge.backend.dto.response;

import java.math.BigDecimal;

public record PortfolioPositionResponse(
        CompanyResponse company,
        int quantity,
        BigDecimal averageCost,
        BigDecimal currentPrice,
        BigDecimal previousClose,
        BigDecimal changePercent,
        BigDecimal marketValue,
        BigDecimal gainLoss,
        BigDecimal gainLossPercent) {}
