package com.quantedge.backend.kafka.dto;

import com.quantedge.backend.enums.OrderSide;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Published to {@code executed-trades} by the order matcher after a fill commits. */
public record TradeExecutedMessage(
        UUID executionId,
        UUID orderId,
        UUID userId,
        String symbol,
        OrderSide side,
        int quantity,
        BigDecimal price,
        Instant executedAt) {}
