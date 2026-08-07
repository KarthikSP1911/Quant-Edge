package com.quantedge.backend.dto.response;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        String symbol,
        OrderSide side,
        int quantity,
        BigDecimal executionPrice,
        OrderStatus status,
        Instant executedAt) {}
