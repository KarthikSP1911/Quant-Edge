package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.quantedge.backend.enums.OrderSide;

public record TransactionResponse(
        UUID id,
        String symbol,
        CompanyResponse company,
        OrderSide side,
        int quantity,
        BigDecimal price,
        String executedAt) {}
