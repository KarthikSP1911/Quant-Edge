package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;

public record OrderResponse(
        UUID id,
        String symbol,
        OrderSide side,
        int quantity,
        BigDecimal executionPrice,
        OrderStatus status,
        Instant executedAt) {}
