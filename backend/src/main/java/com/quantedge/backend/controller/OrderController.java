package com.quantedge.backend.controller;

import com.quantedge.backend.dto.request.MarketOrderRequest;
import com.quantedge.backend.dto.response.OrderResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
}
