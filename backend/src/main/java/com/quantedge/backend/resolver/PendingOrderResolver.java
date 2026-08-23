package com.quantedge.backend.resolver;

import java.math.BigDecimal;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.PendingOrderResponse;
import com.quantedge.backend.entity.PendingAction;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.PendingOrderService;
import com.quantedge.backend.service.QuoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class PendingOrderResolver {

    private final PendingOrderService pendingOrderService;
    private final QuoteService quoteService;

    @QueryMapping
    public PendingOrderResponse pendingOrder(@AuthenticationPrincipal User user) {
        PendingAction action = pendingOrderService.peekAction(user.getId()).orElse(null);
        if (action == null) {
            return null;
        }
        PlaceOrderRequest pending = pendingOrderService.peek(user.getId());

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
                .source(action.getSource())
                .agentRunId(action.getAgentRun() != null ? action.getAgentRun().getId() : null)
                .build();
    }
}
