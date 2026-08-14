package com.quantedge.backend.external.dto;

/** The two fields {@link com.quantedge.backend.service.WalletService} needs back from Stripe. */
public record StripeCheckoutSessionResult(String sessionId, String checkoutUrl) {}
