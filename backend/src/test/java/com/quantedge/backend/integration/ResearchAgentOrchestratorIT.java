package com.quantedge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.entity.AgentStep;
import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AgentRunStatus;
import com.quantedge.backend.enums.AgentStepPhase;
import com.quantedge.backend.external.AlphaVantageClient;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.repository.AgentRunRepository;
import com.quantedge.backend.repository.AgentStepRepository;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.ResearchNoteRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.service.agent.ResearchAgentOrchestrator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

/**
 * Exercises the plan/act/observe loop end to end against a mocked {@link ChatModel}: the model
 * is scripted to request one tool call before giving its final answer, so these tests verify the
 * orchestrator actually executes the tool call via the real {@link
 * com.quantedge.backend.config.ResearchAgentTools} bean, persists an {@link AgentStep} per
 * iteration, and enforces the max-steps guardrail (quantedge.ai.agent.max-steps=3 in the test
 * profile) when the model never stops calling tools.
 */
@SpringBootTest
@ActiveProfiles("test")
class ResearchAgentOrchestratorIT {

    @Autowired
    private ResearchAgentOrchestrator orchestrator;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResearchNoteRepository researchNoteRepository;

    @Autowired
    private AgentRunRepository agentRunRepository;

    @Autowired
    private AgentStepRepository agentStepRepository;

    @MockitoBean
    private AlphaVantageClient alphaVantageClient;

    @MockitoBean
    private FinnhubClient finnhubClient;

    @MockitoBean
    private TwelveDataClient twelveDataClient;

    @MockitoBean
    private ChatModel chatModel;

    private User testUser;

    @BeforeEach
    void setUp() {
        agentStepRepository.deleteAll();
        agentRunRepository.deleteAll();
        researchNoteRepository.deleteAll();
        companyRepository.deleteAll();
        userRepository.deleteAll();

        testUser = userRepository.save(User.builder()
                .email("agent.test@example.com")
                .passwordHash("hash")
                .name("Test Agent")
                .role(com.quantedge.backend.enums.Role.USER)
                .authProvider(com.quantedge.backend.enums.AuthProvider.LOCAL)
                .build());

        companyRepository.save(Company.builder()
                .symbol("NVDA")
                .name("NVIDIA Corp")
                .exchange("NASDAQ")
                .sector("Technology")
                .industry("Semiconductors")
                .description("GPU company")
                .build());

        when(finnhubClient.getQuote(anyString()))
                .thenReturn(new FinnhubQuoteResponse(100.0, 105.0, 95.0, 98.0, 99.0, System.currentTimeMillis()));
        when(twelveDataClient.getTimeSeries(anyString(), anyString(), any(Integer.class)))
                .thenReturn(new TwelveDataTimeSeriesResponse(null, List.of(), "ok", null));
    }

    private AssistantMessage toolCallResponse(String toolName, String argumentsJson) {
        return AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("call-1", "function", toolName, argumentsJson)))
                .build();
    }

    private AssistantMessage finalResponse(String content) {
        return AssistantMessage.builder().content(content).build();
    }

    @Test
    void runResearch_callsProposedToolThenSynthesizesAndSavesReport() {
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(
                        List.of(new Generation(toolCallResponse("getCompanyProfile", "{\"symbol\":\"NVDA\"}")))))
                .thenReturn(new ChatResponse(List.of(new Generation(finalResponse("Mock AI Report")))));

        String sessionId = UUID.randomUUID().toString();
        orchestrator.runResearch(testUser, "NVDA", sessionId);

        List<ResearchNote> notes = researchNoteRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitle()).isEqualTo("Research Report: NVDA");
        assertThat(notes.get(0).getContent()).isEqualTo("Mock AI Report");
        assertThat(notes.get(0).getGeneratedBy()).isEqualTo("AGENT");

        AgentRun run = agentRunRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);
        assertThat(run.getFinalReportId()).isEqualTo(notes.get(0).getId());

        List<AgentStep> steps = agentStepRepository.findByAgentRunIdOrderByStepNumberAsc(run.getId());
        assertThat(steps)
                .extracting(AgentStep::getPhase)
                .contains(AgentStepPhase.TOOL_CALL, AgentStepPhase.OBSERVATION, AgentStepPhase.FINAL);
        assertThat(steps).anySatisfy(step -> assertThat(step.getToolName()).isEqualTo("getCompanyProfile"));
    }

    @Test
    void runResearch_stopsAtMaxStepsAndForcesFinalSynthesis() {
        AssistantMessage repeatingToolCall = toolCallResponse("getRecentNews", "{\"symbol\":\"NVDA\"}");
        when(finnhubClient.getCompanyNews(anyString(), any(), any())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(repeatingToolCall))))
                .thenReturn(new ChatResponse(List.of(new Generation(repeatingToolCall))))
                .thenReturn(new ChatResponse(List.of(new Generation(repeatingToolCall))))
                .thenReturn(new ChatResponse(List.of(new Generation(finalResponse("Forced final report")))));

        String sessionId = UUID.randomUUID().toString();
        orchestrator.runResearch(testUser, "NVDA", sessionId);

        AgentRun run = agentRunRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.MAX_STEPS_REACHED);
        assertThat(run.getStepCount()).isEqualTo(3);

        List<ResearchNote> notes = researchNoteRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getContent()).isEqualTo("Forced final report");
    }

    @Test
    void runResearch_toolFailureIsObservedAsReplanAndRunStillCompletes() {
        when(alphaVantageClient.getIndicator(eq("NVDA"), anyString())).thenThrow(new RuntimeException("Rate limited"));
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(
                        toolCallResponse("getMarketIndicator", "{\"symbol\":\"NVDA\",\"indicator\":\"SMA\"}")))))
                .thenReturn(
                        new ChatResponse(List.of(new Generation(finalResponse("Report noting missing indicator")))));

        String sessionId = UUID.randomUUID().toString();
        orchestrator.runResearch(testUser, "NVDA", sessionId);

        AgentRun run = agentRunRepository.findById(UUID.fromString(sessionId)).orElseThrow();
        assertThat(run.getStatus()).isEqualTo(AgentRunStatus.COMPLETED);

        List<AgentStep> steps = agentStepRepository.findByAgentRunIdOrderByStepNumberAsc(run.getId());
        assertThat(steps).anySatisfy(step -> assertThat(step.getPhase()).isEqualTo(AgentStepPhase.REPLAN));
    }
}
