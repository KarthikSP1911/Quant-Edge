package com.quantedge.backend.dto.response;

public record QuoteResponse(
        double currentPrice, double high, double low, double open, double previousClose, long timestamp) {}
