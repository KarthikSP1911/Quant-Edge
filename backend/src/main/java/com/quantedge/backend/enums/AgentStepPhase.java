package com.quantedge.backend.enums;

/** The phases the agent loop distinguishes in its trace and persisted step log. */
public enum AgentStepPhase {
    PLAN,
    TOOL_CALL,
    OBSERVATION,
    REPLAN,
    VALIDATE,
    AWAITING_APPROVAL,
    FINAL
}
