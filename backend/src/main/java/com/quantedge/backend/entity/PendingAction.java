package com.quantedge.backend.entity;

import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.enums.PendingActionSource;
import com.quantedge.backend.enums.PendingActionStatus;
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
 * A trade proposal an authenticated user has staged - either via the chat agent's
 * {@code placeOrder} tool or via the research agent's {@code proposeTrade} tool - but not yet
 * confirmed or rejected. Durable and audit-capable (unlike the single in-memory slot it replaces),
 * so a proposal survives a restart and a chat proposal and an agent-run proposal for the same user
 * can't silently clobber each other; a partial unique index on {@code (user_id)} where
 * {@code status = 'PENDING'} still enforces one open proposal per user at a time.
 */
@Entity
@Table(name = "pending_actions")
@Data
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class PendingAction {

    @Id
    @Builder.Default
    private UUID id = UUID.randomUUID();

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agent_run_id")
    private AgentRun agentRun;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PendingActionSource source;

    @Column(name = "request_json", nullable = false, columnDefinition = "TEXT")
    private String requestJson;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private PendingActionStatus status = PendingActionStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "resolved_at")
    private Instant resolvedAt;
}
