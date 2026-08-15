package com.quantedge.backend.external.dto;

/** The fields {@link com.quantedge.backend.service.WalletService} needs back from Razorpay. */
public record RazorpayOrderResult(String orderId, long amount, String currency) {}
