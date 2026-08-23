package com.quantedge.backend.service.agent;

import static com.quantedge.backend.enums.AgentRunStatus.AWAITING_APPROVAL;
import static com.quantedge.backend.enums.AgentRunStatus.COMPLETED;
import static com.quantedge.backend.enums.AgentRunStatus.FAILED;
import static com.quantedge.backend.enums.AgentRunStatus.MAX_STEPS_REACHED;
import static com.quantedge.backend.enums.AgentRunStatus.PLANNING;
import static com.quantedge.backend.enums.AgentRunStatus.REPLANNING;
import static com.quantedge.backend.enums.AgentRunStatus.RUNNING;
import static com.quantedge.backend.enums.AgentRunStatus.VALIDATING;

import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.enums.AgentRunStatus;
import com.quantedge.backend.repository.AgentRunRepository;
import org.springframework.stereotype.Component;

/**
 * The single place that changes an {@link AgentRun}'s status, so every transition the orchestrator
 * makes is validated against an explicit table of allowed edges instead of an unchecked
 * {@code run.setStatus(...)} scattered through the loop. This is deliberately a small in-process
 * map, not a workflow engine - the run count and lifecycle here don't warrant one.
 */
@Component
public class AgentRunStateMachine {

    private static final Map<AgentRunStatus, Set<AgentRunStatus>> ALLOWED = new EnumMap<>(AgentRunStatus.class);

    static {
        ALLOWED.put(PLANNING, EnumSet.of(RUNNING, FAILED));
        ALLOWED.put(RUNNING, EnumSet.of(RUNNING, REPLANNING, AWAITING_APPROVAL, VALIDATING, MAX_STEPS_REACHED, FAILED));
        ALLOWED.put(REPLANNING, EnumSet.of(RUNNING, FAILED));
        ALLOWED.put(AWAITING_APPROVAL, EnumSet.of(RUNNING, FAILED));
        ALLOWED.put(VALIDATING, EnumSet.of(COMPLETED, REPLANNING, MAX_STEPS_REACHED, FAILED));
        ALLOWED.put(COMPLETED, EnumSet.noneOf(AgentRunStatus.class));
        ALLOWED.put(FAILED, EnumSet.noneOf(AgentRunStatus.class));
        ALLOWED.put(MAX_STEPS_REACHED, EnumSet.noneOf(AgentRunStatus.class));
    }

    private final AgentRunRepository agentRunRepository;

    public AgentRunStateMachine(AgentRunRepository agentRunRepository) {
        this.agentRunRepository = agentRunRepository;
    }

    /**
     * Moves {@code run} to {@code to}, persisting it, or throws if that edge isn't allowed from the
     * run's current status. FAILED is reachable from every non-terminal state without being listed
     * on each one, since any step in the loop can throw.
     */
    public AgentRun transition(AgentRun run, AgentRunStatus to) {
        AgentRunStatus from = run.getStatus();
        boolean allowed = to == FAILED && !isTerminal(from)
                || ALLOWED.getOrDefault(from, Set.of()).contains(to);
        if (!allowed) {
            throw new IllegalStateException("Illegal agent run transition: " + from + " -> " + to);
        }
        run.setStatus(to);
        return agentRunRepository.save(run);
    }

    private boolean isTerminal(AgentRunStatus status) {
        return status == COMPLETED || status == FAILED || status == MAX_STEPS_REACHED;
    }
}
