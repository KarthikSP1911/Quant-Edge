package com.quantedge.backend.service.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.quantedge.backend.config.AgentGuardrailProperties;
import com.quantedge.backend.config.AgentProposalTools;
import com.quantedge.backend.config.AgentToolRegistry;
import com.quantedge.backend.config.AgentToolRegistry.ToolCategory;
import com.quantedge.backend.config.ReasoningContentSupport;
import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.entity.AgentStep;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.PendingAction;
import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AgentRunStatus;
import com.quantedge.backend.enums.AgentStepPhase;
import com.quantedge.backend.enums.AgentStepStatus;
import com.quantedge.backend.enums.PendingActionStatus;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.repository.AgentRunRepository;
import com.quantedge.backend.repository.AgentStepRepository;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.ResearchNoteRepository;
import com.quantedge.backend.service.PendingOrderService;
import com.quantedge.backend.service.SseTraceService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.model.tool.ToolExecutionResult;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * The research agent's orchestrator: an explicit plan -&gt; act -&gt; observe -&gt; evaluate -&gt; re-plan
 * -&gt; validate -&gt; finish loop, replacing a previous hardcoded 5-step pipeline
 * ({@code fetch profile -> fetch news -> fetch indicators -> synthesize -> save}, always in that
 * order, always calling every data source) with a model that decides for itself which tools it
 * needs and in what order, observes each result, and can react to a failure by trying something
 * else instead of silently degrading a fixed script.
 *
 * <p>Every run begins with an explicit planning turn (tools withheld) before entering the
 * tool-enabled act/observe loop, pulls a short summary of the user's prior completed runs on the
 * same symbol into its opening context as long-term memory, and compacts older tool observations
 * out of the transcript once it grows past a configured step count so the context sent to the
 * model stays bounded. A tool that fails the same way twice in a row triggers an explicit
 * re-planning turn instead of being retried the same way indefinitely; a natural finish is gated by
 * a deterministic validation pass before the run is marked complete. The agent may also propose a
 * trade based on its research - staged via {@link PendingOrderService} exactly like the chat
 * agent's proposals, requiring the user's own out-of-band confirmation - in which case the loop
 * pauses in {@link AgentRunStatus#AWAITING_APPROVAL} until the user resolves it or a timeout elapses.
 *
 * <p>Tool execution is driven manually: {@link ChatModel#call} never auto-executes tool calls on
 * its own, so each round trip is inspected and dispatched through {@link ToolCallingManager}
 * explicitly here. That is what makes each iteration's plan/tool-call/observation visible for
 * {@link AgentStep} persistence and SSE tracing, and what lets the configured max-steps guardrail
 * be enforced as a hard stop rather than an unbounded back-and-forth.
 */
@Slf4j
@Service
public class ResearchAgentOrchestrator {

    private static final String SYSTEM_PROMPT =
            """
            You are QuantEdge's autonomous research agent. Your objective for this run is given in the
            first user message below - it is not always "write a research report"; read it carefully
            and pursue exactly what it asks.

            You work in two stages. First, when asked for a plan, respond ONLY with a short numbered
            plan (2-6 subgoals) for how you will pursue the objective - no tool calls on that turn.
            After that, you enter an act/observe loop: decide which tool you need, call it, observe the
            result, and decide your next step, repeating until you have enough information.

            Tools available: company profile/fundamentals, recent news, technical indicators (SMA, EMA,
            RSI), and a RAG knowledge base search for historical or analyst context beyond the raw data.
            %s

            Decide for yourself which tools you need and in what order - you do not have to call all of
            them, and you may call the same one again with different arguments if the first result was
            insufficient. Observe each tool result before deciding your next step. If a tool fails, do
            not keep retrying the exact same call - try an alternative tool or argument, or proceed with
            an explicit caveat. If you are told a tool has failed repeatedly, change your approach as
            instructed instead of repeating it.

            You have at most %d steps. When you have enough information, respond with your final answer
            and do not call any more tools. If the objective is a research report, write it in Markdown
            with sections: Executive Summary, Fundamentals, Recent Developments, Technical Analysis, and
            Conclusion.
            """;

    private static final String PROPOSE_TRADE_PROMPT_SEGMENT =
            "You may also propose a trade for the user's explicit approval if your research genuinely "
                    + "supports a specific actionable recommendation - it only stages a proposal, it never "
                    + "executes anything, and you must wait for the observation telling you whether the user "
                    + "approved or rejected it before finishing.";

    private static final String DEFAULT_REPORT_GOAL_PREFIX = "Produce a research report for ";
    private static final int MAX_CONSECUTIVE_FAILURES_BEFORE_REPLAN = 2;
    private static final int REPORT_SECTIONS_REQUIRED = 4;
    private static final String[] REPORT_SECTIONS = {
        "Executive Summary", "Fundamentals", "Recent Developments", "Technical Analysis", "Conclusion"
    };

    private final CompanyRepository companyRepository;
    private final ResearchNoteRepository researchNoteRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final SseTraceService sseTraceService;
    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final AgentToolRegistry agentToolRegistry;
    private final AgentRunStateMachine stateMachine;
    private final PendingOrderService pendingOrderService;
    private final AgentGuardrailProperties guardrails;
    private final String model;
    private final Double temperature;
    private final Integer maxTokens;

    public ResearchAgentOrchestrator(
            CompanyRepository companyRepository,
            ResearchNoteRepository researchNoteRepository,
            AgentRunRepository agentRunRepository,
            AgentStepRepository agentStepRepository,
            SseTraceService sseTraceService,
            ChatModel chatModel,
            ToolCallingManager toolCallingManager,
            AgentToolRegistry agentToolRegistry,
            AgentRunStateMachine stateMachine,
            PendingOrderService pendingOrderService,
            AgentGuardrailProperties guardrails,
            @Value("${quantedge.ai.groq.model}") String model,
            @Value("${quantedge.ai.groq.temperature}") Double temperature,
            @Value("${quantedge.ai.groq.max-tokens}") Integer maxTokens) {
        this.companyRepository = companyRepository;
        this.researchNoteRepository = researchNoteRepository;
        this.agentRunRepository = agentRunRepository;
        this.agentStepRepository = agentStepRepository;
        this.sseTraceService = sseTraceService;
        this.chatModel = chatModel;
        this.toolCallingManager = toolCallingManager;
        this.agentToolRegistry = agentToolRegistry;
        this.stateMachine = stateMachine;
        this.pendingOrderService = pendingOrderService;
        this.guardrails = guardrails;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    private final Set<UUID> activeUsers = ConcurrentHashMap.newKeySet();

    public void runResearch(User user, String symbol, String goal, String sessionId) {
        if (!activeUsers.add(user.getId())) {
            sseTraceService.initSession(sessionId, user.getId());
            sendTrace(sessionId, "error", "Another research task is already running for this user.");
            sseTraceService.complete(sessionId);
            return;
        }

        sseTraceService.initSession(sessionId, user.getId());
        AgentRun run = null;
        try {
            Company company = companyRepository
                    .findBySymbol(symbol)
                    .orElseThrow(() -> new CompanyNotFoundException("Symbol not found: " + symbol));

            run = agentRunRepository.save(AgentRun.builder()
                    .id(UUID.fromString(sessionId))
                    .user(user)
                    .company(company)
                    .goal(goal)
                    .status(AgentRunStatus.PLANNING)
                    .maxSteps(guardrails.getMaxSteps())
                    .build());

            executeLoop(run, user, company, symbol, goal);
        } catch (Exception e) {
            log.error("Research agent failed", e);
            sendTrace(sessionId, "error", "Agent failed: " + e.getMessage());
            if (run != null) {
                run.setErrorMessage(e.getMessage());
                run.setCompletedAt(Instant.now());
                stateMachine.transition(run, AgentRunStatus.FAILED);
            }
            sseTraceService.complete(sessionId);
        } finally {
            activeUsers.remove(user.getId());
        }
    }

    private void executeLoop(AgentRun run, User user, Company company, String symbol, String goal) {
        String sessionId = run.getId().toString();

        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(
                String.format(SYSTEM_PROMPT, PROPOSE_TRADE_PROMPT_SEGMENT, guardrails.getMaxSteps())));
        String priorRunsSummary = summarizePriorRuns(user, company);
        if (priorRunsSummary != null) {
            messages.add(new SystemMessage(priorRunsSummary));
        }
        messages.add(new UserMessage("Objective: " + goal + "\nSymbol: " + symbol));

        Set<ToolCategory> toolCategories =
                Set.of(ToolCategory.READ_MARKET_DATA, ToolCategory.READ_RAG, ToolCategory.PROPOSE_TRADE);
        OpenAiChatOptions.Builder optionsBuilder = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .toolContext(Map.of(AgentProposalTools.CONTEXT_USER, user, AgentProposalTools.CONTEXT_AGENT_RUN, run));
        OpenAiChatOptions toolOptions = optionsBuilder
                .toolCallbacks(agentToolRegistry.resolve(toolCategories))
                .build();

        // Explicit planning turn: tools withheld so the model can only produce a plan, not act on it.
        sendTrace(sessionId, "planning", "Drafting a plan for this objective...");
        OpenAiChatOptions planOnlyOptions = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .build();
        messages.add(new UserMessage("First, respond only with your numbered plan for this objective."));
        ChatResponse planResponse =
                chatModel.call(new Prompt(ReasoningContentSupport.strip(messages), planOnlyOptions));
        String plan = planResponse.getResult().getOutput().getText();
        sendTrace(sessionId, "plan", plan);
        persistStep(run, 0, AgentStepPhase.PLAN, null, null, plan, AgentStepStatus.SUCCESS);
        messages.add(new AssistantMessage(plan));
        stateMachine.transition(run, AgentRunStatus.RUNNING);

        String finalReport = null;
        int step = 0;
        long deadline = System.currentTimeMillis() + (guardrails.getRunTimeoutSeconds() * 1000L);
        Map<String, Integer> consecutiveFailures = new HashMap<>();
        boolean validationRetried = false;

        while (step < guardrails.getMaxSteps()) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("Agent run {} exceeded wall-clock budget", run.getId());
                break;
            }
            step++;

            if (step > guardrails.getContextSummarizeAfterSteps()) {
                messages = compactContext(messages);
            }

            sendTrace(sessionId, "planning", "Deciding next action (step " + step + ")...");
            Prompt prompt = new Prompt(ReasoningContentSupport.strip(messages), toolOptions);
            ChatResponse response = chatModel.call(prompt);
            AssistantMessage assistantMessage = response.getResult().getOutput();

            if (assistantMessage.getText() != null
                    && !assistantMessage.getText().isBlank()) {
                sendTrace(sessionId, "plan", assistantMessage.getText());
                persistStep(
                        run,
                        step,
                        AgentStepPhase.PLAN,
                        null,
                        null,
                        assistantMessage.getText(),
                        AgentStepStatus.SUCCESS);
            }

            if (assistantMessage.getToolCalls() == null
                    || assistantMessage.getToolCalls().isEmpty()) {
                finalReport = assistantMessage.getText();
                break;
            }

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                sendTrace(sessionId, "tool_call", toolCall.name() + "(" + toolCall.arguments() + ")");
                persistStep(
                        run,
                        step,
                        AgentStepPhase.TOOL_CALL,
                        toolCall.name(),
                        toolCall.arguments(),
                        null,
                        AgentStepStatus.SUCCESS);
            }

            ToolExecutionResult toolExecutionResult = toolCallingManager.executeToolCalls(prompt, response);
            messages = new ArrayList<>(toolExecutionResult.conversationHistory());

            boolean proposedTrade = false;
            Message lastMessage = messages.get(messages.size() - 1);
            if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
                Integer attemptsUsed = RetryingToolExecutor.lastAttemptCount();
                for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                    boolean failed = toolResponse.responseData() != null
                            && toolResponse.responseData().contains("Error executing");
                    AgentStepStatus status = failed
                            ? AgentStepStatus.FAILURE
                            : (attemptsUsed != null && attemptsUsed > 1
                                    ? AgentStepStatus.RETRIED
                                    : AgentStepStatus.SUCCESS);

                    if (failed) {
                        String key = toolResponse.name();
                        int count = consecutiveFailures.merge(key, 1, Integer::sum);
                        if (count >= MAX_CONSECUTIVE_FAILURES_BEFORE_REPLAN) {
                            consecutiveFailures.remove(key);
                            stateMachine.transition(run, AgentRunStatus.REPLANNING);
                            String replanMessage = "Tool " + toolResponse.name() + " has failed " + count
                                    + " times in a row. Do not retry it the same way - use different "
                                    + "arguments, use an alternative tool, or proceed without that data and "
                                    + "note the gap in your final answer.";
                            sendTrace(sessionId, "replan", replanMessage);
                            persistStep(
                                    run,
                                    step,
                                    AgentStepPhase.REPLAN,
                                    toolResponse.name(),
                                    null,
                                    replanMessage,
                                    AgentStepStatus.FAILURE);
                            messages.add(new UserMessage(replanMessage));
                            stateMachine.transition(run, AgentRunStatus.RUNNING);
                        } else {
                            sendTrace(
                                    sessionId,
                                    "replan",
                                    toolResponse.name() + " -> " + truncate(toolResponse.responseData()));
                            persistStep(
                                    run,
                                    step,
                                    AgentStepPhase.REPLAN,
                                    toolResponse.name(),
                                    null,
                                    toolResponse.responseData(),
                                    status);
                        }
                    } else {
                        consecutiveFailures.remove(toolResponse.name());
                        sendTrace(
                                sessionId,
                                "observation",
                                toolResponse.name() + " -> " + truncate(toolResponse.responseData()));
                        persistStep(
                                run,
                                step,
                                AgentStepPhase.OBSERVATION,
                                toolResponse.name(),
                                null,
                                toolResponse.responseData(),
                                status);
                    }

                    if (!failed && "proposeTrade".equals(toolResponse.name())) {
                        proposedTrade = true;
                    }
                }
            }

            if (proposedTrade) {
                messages = awaitApproval(run, user, sessionId, step, messages);
            }

            run.setStepCount(step);
            agentRunRepository.save(run);
        }

        int nextStepNumber = step + 1;
        AgentRunStatus finalStatus;
        if (finalReport == null) {
            sendTrace(sessionId, "planning", "Step budget reached, synthesizing final report from what's known...");
            messages.add(new UserMessage(
                    "You have used your available steps. Respond now with the best final answer you can "
                            + "produce from the information already gathered, noting any gaps."));
            OpenAiChatOptions forcedOptions = OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
            ChatResponse forcedResponse =
                    chatModel.call(new Prompt(ReasoningContentSupport.strip(messages), forcedOptions));
            finalReport = forcedResponse.getResult().getOutput().getText();
            finalStatus = AgentRunStatus.MAX_STEPS_REACHED;
        } else {
            stateMachine.transition(run, AgentRunStatus.VALIDATING);
            sendTrace(sessionId, "validate", "Checking the final answer is grounded in gathered research...");
            ValidationResult validation = validateReport(run, goal, finalReport);
            persistStep(
                    run,
                    nextStepNumber++,
                    AgentStepPhase.VALIDATE,
                    null,
                    null,
                    validation.reasoning(),
                    validation.passed() ? AgentStepStatus.SUCCESS : AgentStepStatus.FAILURE);

            if (!validation.passed() && !validationRetried && step < guardrails.getMaxSteps()) {
                validationRetried = true;
                stateMachine.transition(run, AgentRunStatus.REPLANNING);
                sendTrace(sessionId, "replan", validation.reasoning());
                messages.add(new AssistantMessage(finalReport));
                messages.add(new UserMessage("Your answer isn't ready yet: " + validation.reasoning()
                        + " Please address this and " + "respond again with your final answer."));
                stateMachine.transition(run, AgentRunStatus.RUNNING);
                ChatResponse retryResponse = chatModel.call(new Prompt(
                        ReasoningContentSupport.strip(messages),
                        OpenAiChatOptions.builder()
                                .model(model)
                                .temperature(temperature)
                                .maxTokens(maxTokens)
                                .build()));
                finalReport = retryResponse.getResult().getOutput().getText();
                stateMachine.transition(run, AgentRunStatus.VALIDATING);
                finalStatus = AgentRunStatus.COMPLETED;
            } else {
                finalStatus = AgentRunStatus.COMPLETED;
            }
        }

        sendTrace(sessionId, "final", finalReport);
        persistStep(run, nextStepNumber, AgentStepPhase.FINAL, null, null, finalReport, AgentStepStatus.SUCCESS);

        sendTrace(sessionId, "saving_report", "Saving final report to database...");
        ResearchNote note = ResearchNote.builder()
                .user(user)
                .company(company)
                .title("Research Report: " + symbol)
                .content(finalReport)
                .generatedBy("AGENT")
                .build();
        note = researchNoteRepository.save(note);

        run.setStepCount(step);
        run.setFinalReportId(note.getId());
        run.setCompletedAt(Instant.now());
        stateMachine.transition(run, finalStatus);

        sendTrace(sessionId, "complete", "Research complete!");
        sseTraceService.complete(sessionId);
    }

    /**
     * Blocks the calling (virtual) thread until the user resolves the trade proposal this run just
     * staged, or {@code approvalTimeoutSeconds} elapses. A virtual thread parking here is cheap, and
     * keeping this single-process/in-loop is simpler and sufficient at this scale - no separate
     * resumable-workflow mechanism is needed.
     */
    private List<Message> awaitApproval(AgentRun run, User user, String sessionId, int step, List<Message> messages) {
        stateMachine.transition(run, AgentRunStatus.AWAITING_APPROVAL);
        sendTrace(sessionId, "awaiting_approval", "Waiting for you to confirm or reject the proposed trade...");
        persistStep(run, step, AgentStepPhase.AWAITING_APPROVAL, null, null, null, AgentStepStatus.SUCCESS);

        long deadline = System.currentTimeMillis() + (guardrails.getApprovalTimeoutSeconds() * 1000L);
        PendingActionStatus resolution = PendingActionStatus.EXPIRED;
        while (System.currentTimeMillis() < deadline) {
            PendingAction action = pendingOrderService.peekAction(user.getId()).orElse(null);
            if (action == null || action.getStatus() != PendingActionStatus.PENDING) {
                resolution = action == null ? PendingActionStatus.EXPIRED : action.getStatus();
                break;
            }
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }

        stateMachine.transition(run, AgentRunStatus.RUNNING);
        String outcome =
                switch (resolution) {
                    case CONFIRMED -> "The user confirmed the proposed trade. It has been executed.";
                    case REJECTED -> "The user rejected the proposed trade. It was not executed.";
                    default -> "The user did not respond to the proposed trade in time; it was not executed.";
                };
        sendTrace(sessionId, "observation", outcome);
        List<Message> updated = new ArrayList<>(messages);
        updated.add(new UserMessage(outcome));
        return updated;
    }

    /** Deterministic (not LLM) validation of the model's final answer before the run is marked complete. */
    private ValidationResult validateReport(AgentRun run, String goal, String finalReport) {
        if (finalReport == null || finalReport.isBlank()) {
            return new ValidationResult(false, "The final answer was empty.");
        }

        long observationCount = agentStepRepository.findByAgentRunIdOrderByStepNumberAsc(run.getId()).stream()
                .filter(s -> s.getPhase() == AgentStepPhase.OBSERVATION && s.getStatus() != AgentStepStatus.FAILURE)
                .count();
        if (observationCount == 0) {
            return new ValidationResult(false, "No successful tool observations were gathered to ground this answer.");
        }

        // Only the default report-generation goal has a fixed expected structure; an arbitrary
        // user-supplied objective may legitimately produce a short, unstructured answer.
        if (goal != null && goal.startsWith(DEFAULT_REPORT_GOAL_PREFIX)) {
            long sectionsPresent =
                    Arrays.stream(REPORT_SECTIONS).filter(finalReport::contains).count();
            if (sectionsPresent < REPORT_SECTIONS_REQUIRED) {
                return new ValidationResult(
                        false,
                        "The report is missing expected sections (found " + sectionsPresent + " of "
                                + REPORT_SECTIONS.length + " - Executive Summary, Fundamentals, Recent "
                                + "Developments, Technical Analysis, Conclusion).");
            }
        }

        return new ValidationResult(true, "Grounded in " + observationCount + " tool observation(s).");
    }

    private record ValidationResult(boolean passed, String reasoning) {}

    /** Short-lived summary of the user's most recent completed runs on this symbol, as long-term memory. */
    private String summarizePriorRuns(User user, Company company) {
        List<AgentRun> priorRuns = agentRunRepository.findByUserIdAndCompanyIdAndStatusOrderByCreatedAtDesc(
                user.getId(), company.getId(), AgentRunStatus.COMPLETED, PageRequest.of(0, 3));
        if (priorRuns.isEmpty()) {
            return null;
        }

        StringBuilder sb = new StringBuilder("Prior research on this symbol for this user, most recent first:\n");
        for (AgentRun prior : priorRuns) {
            if (prior.getFinalReportId() == null) {
                continue;
            }
            researchNoteRepository.findById(prior.getFinalReportId()).ifPresent(note -> {
                String snippet = note.getContent().length() > 500
                        ? note.getContent().substring(0, 500) + "..."
                        : note.getContent();
                sb.append("- (")
                        .append(prior.getCreatedAt())
                        .append(") ")
                        .append(snippet)
                        .append('\n');
            });
        }
        return sb.length() > 60 ? sb.toString() : null;
    }

    /**
     * Deterministically collapses older tool-call/tool-response round trips into one summarizing
     * message once there are more than the last two, keeping the most recent two verbatim so the
     * model still has fresh detail to reason over while the overall context sent to Groq stays
     * bounded. Removes each collapsed assistant tool-call message together with its corresponding
     * tool response as a unit, never one without the other, since a dangling tool_call reference
     * would make the request invalid.
     */
    private List<Message> compactContext(List<Message> messages) {
        List<int[]> pairs = new ArrayList<>();
        for (int i = 0; i < messages.size() - 1; i++) {
            if (messages.get(i) instanceof AssistantMessage am
                    && am.getToolCalls() != null
                    && !am.getToolCalls().isEmpty()
                    && messages.get(i + 1) instanceof ToolResponseMessage) {
                pairs.add(new int[] {i, i + 1});
            }
        }
        if (pairs.size() <= 2) {
            return messages;
        }

        int keepFromPair = pairs.size() - 2;
        Set<Integer> toRemove = new HashSet<>();
        StringBuilder summary = new StringBuilder("[Earlier tool observations summarized]:\n");
        for (int p = 0; p < keepFromPair; p++) {
            int[] pair = pairs.get(p);
            toRemove.add(pair[0]);
            toRemove.add(pair[1]);
            if (messages.get(pair[1]) instanceof ToolResponseMessage trm) {
                for (ToolResponseMessage.ToolResponse r : trm.getResponses()) {
                    summary.append("- ")
                            .append(r.name())
                            .append(": ")
                            .append(truncate(r.responseData()))
                            .append('\n');
                }
            }
        }

        int budget = guardrails.getContextObservationCharBudget();
        String summaryText = summary.length() > budget ? summary.substring(0, budget) + "..." : summary.toString();

        List<Message> compacted = new ArrayList<>();
        boolean inserted = false;
        for (int i = 0; i < messages.size(); i++) {
            if (toRemove.contains(i)) {
                if (!inserted) {
                    compacted.add(new UserMessage(summaryText));
                    inserted = true;
                }
                continue;
            }
            compacted.add(messages.get(i));
        }
        return compacted;
    }

    private void persistStep(
            AgentRun run,
            int stepNumber,
            AgentStepPhase phase,
            String toolName,
            String toolInput,
            String reasoningOrOutput,
            AgentStepStatus status) {
        boolean isOutput = phase == AgentStepPhase.OBSERVATION || phase == AgentStepPhase.REPLAN;
        boolean isReasoning = phase == AgentStepPhase.PLAN
                || phase == AgentStepPhase.FINAL
                || phase == AgentStepPhase.VALIDATE
                || phase == AgentStepPhase.AWAITING_APPROVAL;
        AgentStep step = AgentStep.builder()
                .agentRun(run)
                .stepNumber(stepNumber)
                .phase(phase)
                .toolName(toolName)
                .toolInput(toolInput)
                .toolOutput(isOutput ? reasoningOrOutput : null)
                .reasoning(isReasoning ? reasoningOrOutput : null)
                .status(status)
                .build();
        agentStepRepository.save(step);
    }

    private void sendTrace(String sessionId, String step, String message) {
        sseTraceService.sendEvent(sessionId, "trace", new TraceEvent(step, message));
    }

    private String truncate(String value) {
        if (value == null) {
            return "null";
        }
        return value.length() > 300 ? value.substring(0, 300) + "..." : value;
    }

    public record TraceEvent(String step, String message) {}
}
