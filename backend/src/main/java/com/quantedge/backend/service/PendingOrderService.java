package com.quantedge.backend.service;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.dto.response.PlacedOrderResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.OrderSide;
import com.quantedge.backend.enums.OrderType;
import com.quantedge.backend.exception.InvalidOrderRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Holds the trade an authenticated user has staged (via the chat agent's {@code placeOrder}
 * tool) but not yet executed, and is the single, deterministic path to executing or discarding
 * it.
 *
 * <p>The LLM can only reach {@link #stage}; execution ({@link #confirm}) and discard
 * ({@link #cancel}) are called exclusively from {@code PendingOrderController}, which requires a
 * real authenticated HTTP request from the user's own explicit button click. The model is never
 * given a tool that can move money - it can only ever propose. This is what makes trade
 * authorization deterministic rather than a matter of the system prompt being followed.
 */
@Service
@RequiredArgsConstructor
public class PendingOrderService {

    private final Map<UUID, PlaceOrderRequest> pendingOrders = new ConcurrentHashMap<>();
    private final OrderService orderService;

    public void stage(UUID userId, PlaceOrderRequest request) {
        pendingOrders.put(userId, request);
    }

    public PlaceOrderRequest peek(UUID userId) {
        return pendingOrders.get(userId);
    }

    public void cancel(UUID userId) {
        pendingOrders.remove(userId);
    }

    /** Executes the user's own staged order. Throws if there is nothing staged for them. */
    public Object confirm(User user) {
        PlaceOrderRequest pending = pendingOrders.remove(user.getId());
        if (pending == null) {
            throw new InvalidOrderRequestException("No pending order found to confirm.");
        }

        if (pending.getType() == OrderType.MARKET) {
            OrderResponse response = pending.getSide() == OrderSide.BUY
                    ? orderService.buy(user, pending.getSymbol(), pending.getQuantity())
                    : orderService.sell(user, pending.getSymbol(), pending.getQuantity());
            return response;
        }

        PlacedOrderResponse response = orderService.placeOrder(user, pending);
        return response;
    }
}
