package com.quantedge.backend.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.stereotype.Component;

/**
 * Resolves which tool beans the research orchestrator wires up for a given run, by declared
 * category rather than a single hardcoded {@code toolObjects(researchAgentTools)} call. Today the
 * orchestrator always resolves the full set for a symbol-scoped research goal; this exists as the
 * seam for goal-scoped tool selection (e.g. a purely-informational objective never getting
 * {@code PROPOSE_TRADE}) without another rewrite of the orchestrator's tool wiring.
 */
@Component
public class AgentToolRegistry {

    /** Tool categories the research agent's goal can draw on. */
    public enum ToolCategory {
        READ_MARKET_DATA,
        READ_RAG,
        PROPOSE_TRADE
    }

    private final ResearchAgentTools researchAgentTools;
    private final AgentProposalTools agentProposalTools;

    public AgentToolRegistry(ResearchAgentTools researchAgentTools, AgentProposalTools agentProposalTools) {
        this.researchAgentTools = researchAgentTools;
        this.agentProposalTools = agentProposalTools;
    }

    public List<ToolCallback> resolve(Set<ToolCategory> allowed) {
        List<Object> toolObjects = new ArrayList<>();
        if (allowed.contains(ToolCategory.READ_MARKET_DATA) || allowed.contains(ToolCategory.READ_RAG)) {
            toolObjects.add(researchAgentTools);
        }
        if (allowed.contains(ToolCategory.PROPOSE_TRADE)) {
            toolObjects.add(agentProposalTools);
        }
        return List.of(MethodToolCallbackProvider.builder()
                .toolObjects(toolObjects.toArray())
                .build()
                .getToolCallbacks());
    }
}
