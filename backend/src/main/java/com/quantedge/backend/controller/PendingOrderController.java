package com.quantedge.backend.controller;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.PendingOrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Deterministic authorization for the trade a chat agent has staged via {@code ChatTools#placeOrder}.
 * These endpoints are the only way a staged trade is ever executed or discarded - they require a
 * real authenticated request from the user's own action (the confirmation card's buttons), never a
 * conversational message the LLM could interpret or fabricate on its own.
 */
@RestController
@RequestMapping("/api/v1/chat/pending-order")
@RequiredArgsConstructor
public class PendingOrderController {

    private final PendingOrderService pendingOrderService;

    @PostMapping("/confirm")
    public ResponseEntity<Object> confirm(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(pendingOrderService.confirm(user));
    }

    @PostMapping("/cancel")
    public ResponseEntity<Void> cancel(@AuthenticationPrincipal User user) {
        pendingOrderService.cancel(user.getId());
        return ResponseEntity.noContent().build();
    }
}
