package com.quantedge.backend.enums;

/** The five phases the task explicitly asks the agent loop to distinguish. */
public enum AgentStepPhase {
    PLAN,
    TOOL_CALL,
    OBSERVATION,
    REPLAN,
    FINAL
}
