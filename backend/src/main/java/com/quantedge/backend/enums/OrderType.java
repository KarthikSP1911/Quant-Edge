package com.quantedge.backend.enums;

/** LIMIT, STOP_LOSS and STOP_LIMIT are reserved for Phase 3's order matching engine. */
public enum OrderType {
    MARKET,
    LIMIT,
    STOP_LOSS,
    STOP_LIMIT
}
