package com.quantedge.backend.controller;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.sse.OrderSseRegistry;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/orders")
public class OrderStreamController {

    private final OrderSseRegistry registry;

    public OrderStreamController(OrderSseRegistry registry) {
        this.registry = registry;
    }

    /** Order-fill push stream for the authenticated user. See docs/api-contract.md for the event shape. */
    @GetMapping(path = "/stream", produces = "text/event-stream")
    public SseEmitter stream(@AuthenticationPrincipal User user) {
        return registry.register(user.getId());
    }
}
