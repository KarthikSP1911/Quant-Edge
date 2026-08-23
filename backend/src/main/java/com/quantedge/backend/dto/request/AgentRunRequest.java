package com.quantedge.backend.dto.request;

import jakarta.validation.constraints.Size;

/**
 * Optional free-text objective for a research run. When absent, the orchestrator falls back to
 * the default "produce a research report" goal, so existing callers that only POST the symbol
 * keep working unchanged.
 */
public record AgentRunRequest(@Size(max = 2000) String objective) {}
