package com.quantedge.backend.controller;

import com.quantedge.backend.dto.request.CreateCheckoutSessionRequest;
import com.quantedge.backend.dto.response.CheckoutSessionResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.WalletService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/wallet")
@RequiredArgsConstructor
public class WalletController {

    private final WalletService walletService;

    @PostMapping("/checkout-session")
    public ResponseEntity<CheckoutSessionResponse> createCheckoutSession(
            @AuthenticationPrincipal User user, @Valid @RequestBody CreateCheckoutSessionRequest request) {
        return ResponseEntity.ok(walletService.createCheckoutSession(user, request.getAmountUsd()));
    }
}
