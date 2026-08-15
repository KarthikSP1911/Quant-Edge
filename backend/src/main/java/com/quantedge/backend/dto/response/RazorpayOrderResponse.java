package com.quantedge.backend.dto.response;

public record RazorpayOrderResponse(String orderId, long amount, String currency) {}
