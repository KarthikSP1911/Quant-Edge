package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;

public record OrderSummaryResponse(
        UUID id,
        String symbol,
        CompanyResponse company,
        OrderSide side,
        OrderType type,
        OrderStatus status,
        int quantity,
        int filledQuantity,
        BigDecimal limitPrice,
        BigDecimal stopPrice,
        Instant createdAt,
        Instant updatedAt,
        Instant expiresAt) {}
