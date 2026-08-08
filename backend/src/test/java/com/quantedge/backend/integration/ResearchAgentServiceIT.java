package com.quantedge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

import com.quantedge.backend.entity.Company;
import com.quantedge.backend.entity.ResearchNote;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.external.AlphaVantageClient;
import com.quantedge.backend.external.FinnhubClient;
import com.quantedge.backend.repository.CompanyRepository;
import com.quantedge.backend.repository.ResearchNoteRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.service.ResearchAgentService;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
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

    // We can't easily mock the entire fluent ChatClient API if it's not a mock bean that returns valid intermediate
    // objects.
    // Wait, let's mock it properly. Actually, Spring AI testing often uses a mock ChatClient.Builder or mock
    // ChatClient.
    // A simpler way is to not use @SpringBootTest, but since it's an IT we want the context.
    // Let's just assume we can mock the external clients and see what happens.
    // Actually, in Spring Boot 3.2+ Spring AI, ChatClient is a fluent builder. Mocking it is hard.
    // Let's mock the underlying ChatModel instead.
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

        when(finnhubClient.getCompanyNews(anyString(), any(), any())).thenReturn(List.of());
        when(chatModel.call(any(Prompt.class)))
                .thenReturn(new ChatResponse(List.of(new Generation(new AssistantMessage("Mock AI Report")))));
    }

    @Test
    void runResearch_successWithFullData() {
        when(alphaVantageClient.getIndicator(eq("NVDA"), anyString())).thenReturn("Mock Indicator Data");

        researchAgentService.runResearch(testUser, "NVDA", UUID.randomUUID().toString());

        List<ResearchNote> notes = researchNoteRepository.findByUserOrderByCreatedAtDesc(testUser);
        assertThat(notes).hasSize(1);
        assertThat(notes.get(0).getTitle()).isEqualTo("Research Report: NVDA");
        assertThat(notes.get(0).getContent()).contains("Mock AI Report");
        assertThat(notes.get(0).getGeneratedBy()).isEqualTo("AGENT");
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
