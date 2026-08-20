package com.quantedge.backend.entity;

import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.enums.AgentStepPhase;
import com.quantedge.backend.enums.AgentStepStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

/**
 * One recorded iteration of an {@link AgentRun}'s loop - a plan, a tool call, an observation, a
 * replan triggered by a failure, or the final synthesized response. This is the agent's
 * short-term memory (replayed back into the LLM context within a run) and, once persisted,
 * its long-term memory (inspectable after the run completes).
 */
@Entity
@Table(name = "agent_steps")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AgentStep {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "agent_run_id", nullable = false)
    @ToString.Exclude
    private AgentRun agentRun;

    @Column(name = "step_number", nullable = false)
    private int stepNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentStepPhase phase;

    @Column(name = "tool_name", length = 100)
    private String toolName;

    @Column(name = "tool_input", columnDefinition = "TEXT")
    private String toolInput;

    @Column(name = "tool_output", columnDefinition = "TEXT")
    private String toolOutput;

    @Column(columnDefinition = "TEXT")
    private String reasoning;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentStepStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();
}
