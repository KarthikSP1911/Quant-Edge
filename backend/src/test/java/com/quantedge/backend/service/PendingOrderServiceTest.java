package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderStatus;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.enums.TimeInForce;
import com.quantedge.backend.exception.InvalidOrderRequestException;
import com.quantedge.backend.repository.PendingActionRepository;
import com.quantedge.backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;

/**
 * {@link PendingOrderService} is the sole path by which a staged trade - from the chat agent's
 * {@code placeOrder} tool or the research agent's {@code proposeTrade} tool - is executed or
 * discarded - the LLM can only reach {@link PendingOrderService#stage}, never
 * {@link PendingOrderService#confirm}. These tests pin down that a staged order only executes
 * when {@code confirm} is called directly (i.e. from the deterministic REST endpoint, never from
 * tool-calling), that confirming twice or confirming nothing fails loudly instead of silently
 * double-executing, and that cancel discards without ever touching OrderService. Runs against a
 * real (H2) {@link PendingActionRepository} via {@code @DataJpaTest} since the service is now
 * persistence-backed rather than an in-memory map.
 */
@DataJpaTest
class PendingOrderServiceTest {

    @Autowired
    private PendingActionRepository pendingActionRepository;

    @Autowired
    private UserRepository userRepository;

    private final OrderService orderService = mock(OrderService.class);

    private PendingOrderService pendingOrderService;
    private User user;

    @BeforeEach
    void setUp() {
        pendingOrderService =
                new PendingOrderService(pendingActionRepository, userRepository, orderService, new ObjectMapper());
        user = userRepository.save(User.builder()
                .email("pending-order-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .name("Test User")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build());
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
        User otherUser = userRepository.save(User.builder()
                .email("pending-order-other-" + UUID.randomUUID() + "@example.com")
                .passwordHash("hash")
                .name("Other User")
                .role(Role.USER)
                .authProvider(AuthProvider.LOCAL)
                .build());
        pendingOrderService.stage(user.getId(), marketOrder());

        assertThat(pendingOrderService.peek(user.getId())).isNotNull();
        assertThat(pendingOrderService.peek(otherUser.getId())).isNull();
    }

    @Test
    void staging_expiresAnyPreviousPendingProposalForThatUser() {
        pendingOrderService.stage(user.getId(), marketOrder());
        PlaceOrderRequest second = PlaceOrderRequest.builder()
                .symbol("MSFT")
                .side(OrderSide.SELL)
                .type(OrderType.MARKET)
                .quantity(2)
                .timeInForce(TimeInForce.DAY)
                .build();

        pendingOrderService.stage(user.getId(), second);

        assertThat(pendingOrderService.peek(user.getId()).getSymbol()).isEqualTo("MSFT");
    }
}
