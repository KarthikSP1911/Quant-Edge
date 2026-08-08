package com.quantedge.backend.controller;

import java.util.UUID;

import com.quantedge.backend.dto.request.MarketOrderRequest;
import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.dto.response.PlacedOrderResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping("/buy")
    public ResponseEntity<OrderResponse> buy(
            @AuthenticationPrincipal User user, @Valid @RequestBody MarketOrderRequest request) {
        return ResponseEntity.ok(orderService.buy(user, request.getSymbol(), request.getQuantity()));
    }

    @PostMapping("/sell")
    public ResponseEntity<OrderResponse> sell(
            @AuthenticationPrincipal User user, @Valid @RequestBody MarketOrderRequest request) {
        return ResponseEntity.ok(orderService.sell(user, request.getSymbol(), request.getQuantity()));
    }

    @PostMapping
    public ResponseEntity<PlacedOrderResponse> place(
            @AuthenticationPrincipal User user, @Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(orderService.placeOrder(user, request));
    }

    @PostMapping("/{orderId}/cancel")
    public ResponseEntity<PlacedOrderResponse> cancel(@AuthenticationPrincipal User user, @PathVariable UUID orderId) {
        return ResponseEntity.ok(orderService.cancelOrder(user, orderId));
    }
}
