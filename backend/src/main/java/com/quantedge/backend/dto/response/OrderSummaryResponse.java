package com.quantedge.backend.dto.response;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderSummaryResponse(
        UUID id,
        String symbol,
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
