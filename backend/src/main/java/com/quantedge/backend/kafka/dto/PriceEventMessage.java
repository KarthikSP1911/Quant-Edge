package com.quantedge.backend.kafka.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Published to {@code stock-prices} after every successful per-symbol Finnhub sync. */
public record PriceEventMessage(String symbol, BigDecimal price, Instant syncedAt) {}
