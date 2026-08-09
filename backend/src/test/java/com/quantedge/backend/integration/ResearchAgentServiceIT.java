package com.quantedge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.AlphaVantageClient;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.external.TwelveDataClient;
import com.quantedge.backend.external.dto.FinnhubQuoteResponse;
import com.quantedge.backend.external.dto.TwelveDataTimeSeriesResponse;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.ResearchNoteRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.service.ResearchAgentService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.client.RestClientException;

@SpringBootTest
@ActiveProfiles("test")
class ResearchAgentServiceIT {

    @Autowired
    private ResearchAgentService researchAgentService;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CompanyRepository companyRepository;

    @Autowired
    private ResearchNoteRepository researchNoteRepository;

    @MockitoBean
    private AlphaVantageClient alphaVantageClient;

    @MockitoBean
    private FinnhubClient finnhubClient;

    @MockitoBean
    private TwelveDataClient twelveDataClient;

    @MockitoBean
    private org.springframework.ai.chat.model.ChatModel chatModel;

    private User testUser;
    private Company testCompany;

    @BeforeEach
    void setUp() {
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

        testCompany = companyRepository.save(Company.builder()
                .symbol("NVDA")
                .name("NVIDIA Corp")
                .exchange("NASDAQ")
                .sector("Technology")
                .industry("Semiconductors")
                .description("GPU company")
                .build());

        // Mock StockDetailService dependencies
        when(finnhubClient.getQuote(anyString()))
                .thenReturn(new FinnhubQuoteResponse(100.0, 105.0, 95.0, 98.0, 99.0, System.currentTimeMillis()));
        when(twelveDataClient.getTimeSeries(anyString(), anyString(), any(Integer.class)))
                .thenReturn(new TwelveDataTimeSeriesResponse(null, List.of(), "ok", null));

        when(finnhubClient.getCompanyNews(anyString(), any(), any())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock AI Report")))));
    }

    @Test
    void runResearch_successWithFullData_inOrder() {
        when(alphaVantageClient.getIndicator(eq("NVDA"), anyString())).thenReturn("Mock Indicator Data");

        researchAgentService.runResearch(testUser, "NVDA", UUID.randomUUID().toString());

        List<ResearchNote> notes = researchNoteRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitle()).isEqualTo("Research Report: NVDA");
        assertThat(notes.get(0).getContent()).contains("Mock AI Report");
        assertThat(notes.get(0).getGeneratedBy()).isEqualTo("AGENT");

        InOrder inOrder = inOrder(finnhubClient, alphaVantageClient, chatModel);

        // 1. StockDetail (Quote) - FinnhubClient
        inOrder.verify(finnhubClient).getQuote(eq("NVDA"));

        // 2. News - FinnhubClient
        inOrder.verify(finnhubClient).getCompanyNews(eq("NVDA"), any(), any());

        // 3. Indicators - AlphaVantageClient
        inOrder.verify(alphaVantageClient).getIndicator(eq("NVDA"), eq("SMA"));
        inOrder.verify(alphaVantageClient).getIndicator(eq("NVDA"), eq("RSI"));

        // 4. LLM Synthesis - ChatModel
        inOrder.verify(chatModel).call(any(Prompt.class));
    }

    @Test
    void runResearch_gracefulDegradationOn429() {
        doThrow(new ExternalApiException("Rate limited", new RestClientException("429 Too Many Requests")))
                .when(alphaVantageClient)
                .getIndicator(anyString(), anyString());

        researchAgentService.runResearch(testUser, "NVDA", UUID.randomUUID().toString());

        List<ResearchNote> notes = researchNoteRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getContent()).contains("Mock AI Report");
        // The service should still generate the report despite AlphaVantage throwing exception.
    }
}
