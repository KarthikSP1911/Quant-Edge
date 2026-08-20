package com.quantedge.backend.service.agent;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.quantedge.backend.config.AgentGuardrailProperties;
import com.quantedge.backend.config.ResearchAgentTools;
import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.entity.AgentStep;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AgentRunStatus;
import com.quantedge.backend.enums.AgentStepPhase;
import com.quantedge.backend.enums.AgentStepStatus;
import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.repository.AgentRunRepository;
import com.quantedge.backend.repository.AgentStepRepository;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.ResearchNoteRepository;
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
import org.springframework.ai.tool.method.MethodToolCallbackProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * The research agent's orchestrator: an explicit plan / act / observe loop over
 * {@link ResearchAgentTools}, replacing the previous hardcoded 5-step pipeline
 * ({@code fetch profile -> fetch news -> fetch indicators -> synthesize -> save}, always in that
 * order, always calling every data source) with a model that decides for itself which tools it
 * needs and in what order, observes each result, and can react to a failure by trying something
 * else instead of silently degrading a fixed script.
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
            You are QuantEdge's autonomous research agent. Your goal is to produce a comprehensive,
            well-grounded research report on the given stock symbol.

            You have tools to fetch the company profile, recent news, technical indicators, and to
            search a knowledge base of ingested news and analysis (RAG) when you need more context,
            historical grounding, or analyst commentary than the raw data alone gives you. Decide for
            yourself which tools you actually need and in what order - you do not have to call all of
            them, and you may call the same one again with different arguments if the first result was
            insufficient.

            Observe each tool result before deciding your next step. If a tool fails or returns
            insufficient data, do not stop - note the gap and either try an alternative tool/argument or
            proceed with an explicit caveat in the final report.

            You have at most %d steps. When you have enough information, respond with the final report
            in Markdown with sections: Executive Summary, Fundamentals, Recent Developments, Technical
            Analysis, and Conclusion. Do not call any more tools once you are ready to give the final
            report - simply write it as your response.
            """;

    private final CompanyRepository companyRepository;
    private final ResearchNoteRepository researchNoteRepository;
    private final AgentRunRepository agentRunRepository;
    private final AgentStepRepository agentStepRepository;
    private final SseTraceService sseTraceService;
    private final ChatModel chatModel;
    private final ToolCallingManager toolCallingManager;
    private final ResearchAgentTools researchAgentTools;
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
            ResearchAgentTools researchAgentTools,
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
        this.researchAgentTools = researchAgentTools;
        this.guardrails = guardrails;
        this.model = model;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
    }

    private final Set<UUID> activeUsers = ConcurrentHashMap.newKeySet();

    public void runResearch(User user, String symbol, String sessionId) {
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
                    .goal("Produce a research report for " + symbol)
                    .status(AgentRunStatus.RUNNING)
                    .maxSteps(guardrails.getMaxSteps())
                    .build());

            executeLoop(run, user, company, symbol);
        } catch (Exception e) {
            log.error("Research agent failed", e);
            sendTrace(sessionId, "error", "Agent failed: " + e.getMessage());
            if (run != null) {
                run.setStatus(AgentRunStatus.FAILED);
                run.setErrorMessage(e.getMessage());
                run.setCompletedAt(Instant.now());
                agentRunRepository.save(run);
            }
            sseTraceService.complete(sessionId);
        } finally {
            activeUsers.remove(user.getId());
        }
    }

    private void executeLoop(AgentRun run, User user, Company company, String symbol) {
        String sessionId = run.getId().toString();
        List<Message> messages = new ArrayList<>();
        messages.add(new SystemMessage(String.format(SYSTEM_PROMPT, guardrails.getMaxSteps())));
        messages.add(new UserMessage("Research symbol: " + symbol));

        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .toolCallbacks(MethodToolCallbackProvider.builder()
                        .toolObjects(researchAgentTools)
                        .build()
                        .getToolCallbacks())
                .build();

        String finalReport = null;
        int step = 0;
        long deadline = System.currentTimeMillis() + (guardrails.getRunTimeoutSeconds() * 1000L);

        while (step < guardrails.getMaxSteps()) {
            if (System.currentTimeMillis() > deadline) {
                log.warn("Agent run {} exceeded wall-clock budget", run.getId());
                break;
            }
            step++;

            sendTrace(sessionId, "planning", "Deciding next action (step " + step + ")...");
            Prompt prompt = new Prompt(messages, options);
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

            Message lastMessage = messages.get(messages.size() - 1);
            if (lastMessage instanceof ToolResponseMessage toolResponseMessage) {
                for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                    boolean failed = toolResponse.responseData() != null
                            && toolResponse.responseData().contains("Error executing");
                    String eventName = failed ? "replan" : "observation";
                    sendTrace(
                            sessionId, eventName, toolResponse.name() + " -> " + truncate(toolResponse.responseData()));
                    persistStep(
                            run,
                            step,
                            failed ? AgentStepPhase.REPLAN : AgentStepPhase.OBSERVATION,
                            toolResponse.name(),
                            null,
                            toolResponse.responseData(),
                            failed ? AgentStepStatus.FAILURE : AgentStepStatus.SUCCESS);
                }
            }

            run.setStepCount(step);
            agentRunRepository.save(run);
        }

        AgentRunStatus finalStatus = AgentRunStatus.COMPLETED;
        if (finalReport == null) {
            sendTrace(sessionId, "planning", "Step budget reached, synthesizing final report from what's known...");
            messages.add(new UserMessage(
                    "You have used your available steps. Respond now with the best final report you can "
                            + "produce from the information already gathered, noting any gaps."));
            OpenAiChatOptions forcedOptions = OpenAiChatOptions.builder()
                    .model(model)
                    .temperature(temperature)
                    .maxTokens(maxTokens)
                    .build();
            ChatResponse forcedResponse = chatModel.call(new Prompt(messages, forcedOptions));
            finalReport = forcedResponse.getResult().getOutput().getText();
            finalStatus = AgentRunStatus.MAX_STEPS_REACHED;
        }

        sendTrace(sessionId, "final", finalReport);
        persistStep(run, step + 1, AgentStepPhase.FINAL, null, null, finalReport, AgentStepStatus.SUCCESS);

        sendTrace(sessionId, "saving_report", "Saving final report to database...");
        ResearchNote note = ResearchNote.builder()
                .user(user)
                .company(company)
                .title("Research Report: " + symbol)
                .content(finalReport)
                .generatedBy("AGENT")
                .build();
        note = researchNoteRepository.save(note);

        run.setStatus(finalStatus);
        run.setStepCount(step);
        run.setFinalReportId(note.getId());
        run.setCompletedAt(Instant.now());
        agentRunRepository.save(run);

        sendTrace(sessionId, "complete", "Research complete!");
        sseTraceService.complete(sessionId);
    }

    private void persistStep(
            AgentRun run,
            int stepNumber,
            AgentStepPhase phase,
            String toolName,
            String toolInput,
            String reasoningOrOutput,
            AgentStepStatus status) {
        AgentStep step = AgentStep.builder()
                .agentRun(run)
                .stepNumber(stepNumber)
                .phase(phase)
                .toolName(toolName)
                .toolInput(toolInput)
                .toolOutput(
                        phase == AgentStepPhase.OBSERVATION || phase == AgentStepPhase.REPLAN
                                ? reasoningOrOutput
                                : null)
                .reasoning(phase == AgentStepPhase.PLAN || phase == AgentStepPhase.FINAL ? reasoningOrOutput : null)
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
