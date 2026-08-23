package com.quantedge.backend.dto.response;

import java.math.BigDecimal;
import java.util.UUID;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.PendingActionSource;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PendingOrderResponse {
    private String symbol;
    private OrderSide side;
    private OrderType type;
    private int quantity;
    private BigDecimal limitPrice;
    private BigDecimal stopPrice;
    private BigDecimal estimatedCost;
    private PendingActionSource source;
    private UUID agentRunId;
}
