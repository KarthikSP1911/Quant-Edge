package com.quantedge.backend.enums;

public enum OrderStatus {
    PENDING,
    /** Resting on the book, awaiting its trigger condition (LIMIT/STOP_LOSS/STOP_LIMIT orders). */
    OPEN,
    FILLED,
    PARTIALLY_FILLED,
    CANCELLED,
    REJECTED,
    EXPIRED
}
