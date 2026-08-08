package com.quantedge.backend.dto.response;

import java.math.BigDecimal;

public record TimeMachineHoldingResponse(
        CompanyResponse company,
        int quantity,
        BigDecimal averageCost,
        BigDecimal priceAtDate,
        BigDecimal marketValue,
        BigDecimal gainLoss,
        BigDecimal gainLossPercent) {}
