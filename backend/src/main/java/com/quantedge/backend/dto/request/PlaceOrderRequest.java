package com.quantedge.backend.dto.request;

import java.math.BigDecimal;

import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.TimeInForce;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Places a resting LIMIT/STOP_LOSS/STOP_LIMIT order. Market orders keep using
 * /api/orders/buy|sell, which execute synchronously instead of resting on the book.
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceOrderRequest {
    @NotBlank
    private String symbol;

    @NotNull private OrderSide side;

    @NotNull private OrderType type;

    @Min(1)
    private int quantity;

    @DecimalMin(value = "0.01")
    private BigDecimal limitPrice;

    @DecimalMin(value = "0.01")
    private BigDecimal stopPrice;

    @NotNull private TimeInForce timeInForce;
}
