package com.quantedge.backend.resolver;

import java.math.BigDecimal;

import com.quantedge.backend.config.ChatTools;
import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.PendingOrderResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PendingOrderResolver {

    private final ChatTools chatTools;
    private final QuoteService quoteService;

    @QueryMapping
    public PendingOrderResponse pendingOrder(@AuthenticationPrincipal User user) {
        PlaceOrderRequest pending = chatTools.peekPendingOrder(user.getId());
        if (pending == null) {
            return null;
        }

        BigDecimal unitPrice = pending.getLimitPrice();
        if (unitPrice == null) {
            try {
                unitPrice = BigDecimal.valueOf(
                        quoteService.getQuote(pending.getSymbol()).currentPrice());
            } catch (Exception e) {
                unitPrice = null;
            }
        }
        BigDecimal estimatedCost =
                unitPrice == null ? null : unitPrice.multiply(BigDecimal.valueOf(pending.getQuantity()));

        return PendingOrderResponse.builder()
                .symbol(pending.getSymbol())
                .side(pending.getSide())
                .type(pending.getType())
                .quantity(pending.getQuantity())
                .limitPrice(pending.getLimitPrice())
                .stopPrice(pending.getStopPrice())
                .estimatedCost(estimatedCost)
                .build();
    }
}
