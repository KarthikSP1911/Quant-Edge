package com.quantedge.backend.entity;

import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.enums.AgentRunStatus;
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

/**
 * Durable state for one agent orchestrator run - the "task state" the agent loop reads and
 * writes across planning/execution/observation iterations, and the long-term memory a future run
 * or the user can look back on after the in-memory SSE trace session has expired.
 */
@Entity
@Table(name = "agent_runs")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class AgentRun {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String goal;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AgentRunStatus status;

    @Column(name = "max_steps", nullable = false)
    private int maxSteps;

    @Column(name = "step_count", nullable = false)
    @Builder.Default
    private int stepCount = 0;

    @Column(name = "final_report_id")
    private UUID finalReportId;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "completed_at")
    private Instant completedAt;
}
