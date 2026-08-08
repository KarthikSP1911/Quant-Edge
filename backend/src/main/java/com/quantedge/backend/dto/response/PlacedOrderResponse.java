package com.quantedge.backend.dto.response;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.TimeInForce;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PlacedOrderResponse(
        UUID id,
        String symbol,
        OrderSide side,
        OrderType type,
        int quantity,
        BigDecimal limitPrice,
        BigDecimal stopPrice,
        TimeInForce timeInForce,
        OrderStatus status,
        Instant expiresAt,
        Instant createdAt) {}
