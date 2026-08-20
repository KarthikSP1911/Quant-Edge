package com.quantedge.backend.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Guardrails for the research agent's plan/execute/observe loop: how many iterations it may run,
 * how long a single tool call may take before it's treated as a failed observation, and how many
 * times a failed tool call is retried before the failure is surfaced to the model to react to.
 */
@Component
@ConfigurationProperties(prefix = "quantedge.ai.agent")
public class AgentGuardrailProperties {

    /** Hard cap on plan/act/observe iterations before the agent is forced to a final synthesis. */
    private int maxSteps = 8;

    /** Per tool-call timeout, in seconds, before it's treated as a failed observation. */
    private int toolTimeoutSeconds = 15;

    /** Max retries for a single tool call after a transient failure (e.g. an upstream 429). */
    private int toolMaxRetries = 2;

    /** Wall-clock budget for the whole run, in seconds, independent of step count. */
    private int runTimeoutSeconds = 120;

    public int getMaxSteps() {
        return maxSteps;
    }

    public void setMaxSteps(int maxSteps) {
        this.maxSteps = maxSteps;
    }

    public int getToolTimeoutSeconds() {
        return toolTimeoutSeconds;
    }

    public void setToolTimeoutSeconds(int toolTimeoutSeconds) {
        this.toolTimeoutSeconds = toolTimeoutSeconds;
    }

    public int getToolMaxRetries() {
        return toolMaxRetries;
    }

    public void setToolMaxRetries(int toolMaxRetries) {
        this.toolMaxRetries = toolMaxRetries;
    }

    public int getRunTimeoutSeconds() {
        return runTimeoutSeconds;
    }

    public void setRunTimeoutSeconds(int runTimeoutSeconds) {
        this.runTimeoutSeconds = runTimeoutSeconds;
    }
}
