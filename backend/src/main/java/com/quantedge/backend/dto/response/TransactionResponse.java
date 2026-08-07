package com.quantedge.backend.dto.response;

import com.quantedge.backend.enums.OrderSide;
import java.math.BigDecimal;
import java.util.UUID;

public record TransactionResponse(
        UUID id, String symbol, OrderSide side, int quantity, BigDecimal price, String executedAt) {}
