package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.TimeInForce;
import com.quantedge.backend.exception.InvalidOrderRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * {@link PendingOrderService} is the sole path by which a chat-agent-staged trade is executed or
 * discarded - the LLM's {@code placeOrder} tool can only reach {@link PendingOrderService#stage},
 * never {@link PendingOrderService#confirm}. These tests pin down that a staged order only
 * executes when {@code confirm} is called directly (i.e. from the deterministic REST endpoint,
 * never from tool-calling), that confirming twice or confirming nothing fails loudly instead of
 * silently double-executing, and that cancel discards without ever touching OrderService.
 */
@ExtendWith(MockitoExtension.class)
class PendingOrderServiceTest {

    @Mock
    private OrderService orderService;

    private PendingOrderService pendingOrderService;
    private User user;

    @BeforeEach
    void setUp() {
        pendingOrderService = new PendingOrderService(orderService);
        user = User.builder().id(UUID.randomUUID()).build();
    }

    private PlaceOrderRequest marketOrder() {
        return PlaceOrderRequest.builder()
                .symbol("AAPL")
                .side(OrderSide.BUY)
                .type(OrderType.MARKET)
                .quantity(5)
                .timeInForce(TimeInForce.DAY)
                .build();
    }

    @Test
    void stageThenConfirm_executesTheStagedMarketOrderExactlyOnce() {
        pendingOrderService.stage(user.getId(), marketOrder());
        OrderResponse response =
                new OrderResponse(UUID.randomUUID(), "AAPL", OrderSide.BUY, 5, null, OrderStatus.FILLED, null);
        when(orderService.buy(user, "AAPL", 5)).thenReturn(response);

        Object result = pendingOrderService.confirm(user);

        assertThat(result).isEqualTo(response);
        verify(orderService).buy(user, "AAPL", 5);

        assertThatThrownBy(() -> pendingOrderService.confirm(user)).isInstanceOf(InvalidOrderRequestException.class);
    }

    @Test
    void confirm_withNothingStaged_throwsAndNeverCallsOrderService() {
        assertThatThrownBy(() -> pendingOrderService.confirm(user)).isInstanceOf(InvalidOrderRequestException.class);

        verifyNoInteractions(orderService);
    }

    @Test
    void cancel_discardsTheStagedOrderWithoutTouchingOrderService() {
        pendingOrderService.stage(user.getId(), marketOrder());

        pendingOrderService.cancel(user.getId());

        assertThat(pendingOrderService.peek(user.getId())).isNull();
        verifyNoInteractions(orderService);
        assertThatThrownBy(() -> pendingOrderService.confirm(user)).isInstanceOf(InvalidOrderRequestException.class);
    }

    @Test
    void peek_reflectsTheMostRecentlyStagedOrderForThatUserOnly() {
        User otherUser = User.builder().id(UUID.randomUUID()).build();
        pendingOrderService.stage(user.getId(), marketOrder());

        assertThat(pendingOrderService.peek(user.getId())).isNotNull();
        assertThat(pendingOrderService.peek(otherUser.getId())).isNull();
    }
}
