package com.quantedge.backend.dto.response;

public record CandleResponse(String datetime, double open, double high, double low, double close, double volume) {}
