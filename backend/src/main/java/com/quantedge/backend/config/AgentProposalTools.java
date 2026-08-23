package com.quantedge.backend.config;

import com.quantedge.backend.dto.request.PlaceOrderRequest;
import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.PendingActionSource;
import com.quantedge.backend.service.PendingOrderService;
import com.quantedge.backend.service.agent.RetryingToolExecutor;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

/**
 * The one write-capable tool the research agent may call, kept in its own bean (separate from the
 * read-only {@link ResearchAgentTools}) so the permission boundary stays visible in the wiring: a
 * chat model configured with {@link ResearchAgentTools} alone still cannot touch this. Delegates to
 * the exact same {@link PendingOrderService#stage} the chat agent's {@code ChatTools#placeOrder}
 * uses - there is one staging path, not two - so the deterministic "LLM proposes, an authenticated
 * REST call is the only way to execute" boundary from {@link PendingOrderService} applies here
 * unchanged.
 *
 * <p>{@code user}/{@code agentRun} arrive via Spring AI's {@link ToolContext} (populated by
 * {@code ResearchAgentOrchestrator} on the {@code OpenAiChatOptions} for the run), not as ordinary
 * tool parameters - the model never sees or supplies them, since it has no legitimate way to know
 * whose trade it's proposing beyond "whoever's research run this is."
 */
@Component
public class AgentProposalTools {

    public static final String CONTEXT_USER = "user";
    public static final String CONTEXT_AGENT_RUN = "agentRun";

    private final PendingOrderService pendingOrderService;
    private final RetryingToolExecutor retryingToolExecutor;

    public AgentProposalTools(PendingOrderService pendingOrderService, RetryingToolExecutor retryingToolExecutor) {
        this.pendingOrderService = pendingOrderService;
        this.retryingToolExecutor = retryingToolExecutor;
    }

    @Tool(
            description = "Propose a trade based on your research (e.g. recommending the user buy/sell given your "
                    + "analysis). This never executes anything - it only stages a proposal that requires the user's "
                    + "own explicit confirmation via a card shown in the UI; you have no way to execute or discard it "
                    + "yourself. Only call this if your research genuinely supports a specific actionable "
                    + "recommendation, not speculatively. After calling this, wait for the observation telling you "
                    + "whether the user confirmed or rejected it before finishing your report.")
    public Object proposeTrade(PlaceOrderRequest request, ToolContext toolContext) {
        try {
            User user = (User) toolContext.getContext().get(CONTEXT_USER);
            AgentRun agentRun = (AgentRun) toolContext.getContext().get(CONTEXT_AGENT_RUN);
            retryingToolExecutor.execute(() -> {
                pendingOrderService.stage(user.getId(), request, PendingActionSource.AGENT, agentRun);
                return null;
            });
            return "Trade proposed: " + request.getSide() + " " + request.getQuantity() + " of " + request.getSymbol()
                    + ". Waiting for the user to accept or reject it.";
        } catch (Exception e) {
            return "Error staging trade proposal: " + e.getMessage();
        }
    }
}
