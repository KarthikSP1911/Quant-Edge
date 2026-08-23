package com.quantedge.backend.enums;

/**
 * A run's lifecycle. {@code PLANNING} and {@code VALIDATING} are short-lived, set immediately
 * around their respective LLM/deterministic calls; {@code AWAITING_APPROVAL} and
 * {@code REPLANNING} actually pause or redirect the loop and are the two states a caller polling
 * an {@link com.quantedge.backend.entity.AgentRun} should treat as "still active, but not making
 * forward progress on its own right now."
 */
public enum AgentRunStatus {
    PLANNING,
    RUNNING,
    AWAITING_APPROVAL,
    VALIDATING,
    REPLANNING,
    COMPLETED,
    FAILED,
    MAX_STEPS_REACHED
}
