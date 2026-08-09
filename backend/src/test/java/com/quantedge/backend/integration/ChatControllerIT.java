package com.quantedge.backend.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantedge.backend.dto.request.ChatRequest;
import com.quantedge.backend.entity.ChatHistory;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.enums.AuthProvider;
import com.quantedge.backend.enums.Role;
import com.quantedge.backend.repository.ChatHistoryRepository;
import com.quantedge.backend.repository.UserRepository;
import com.quantedge.backend.security.JwtService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@SpringBootTest
@Testcontainers
class ChatControllerIT {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:15-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        // Mock Groq API Key to prevent errors
        registry.add("spring.ai.openai.api-key", () -> "dummy");
        // Use an empty bootstrap servers to disable real Kafka connection
        registry.add("spring.kafka.bootstrap-servers", () -> "");
        registry.add("spring.kafka.admin.fail-fast", () -> "false");
    }

    @Autowired
    private WebApplicationContext context;

    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private ChatHistoryRepository chatHistoryRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private ChatClient chatClient;

    @TestConfiguration
    static class MockConfig {
        @Bean
        @Primary
        public ChatClient chatClient() {
            return Mockito.mock(ChatClient.class, Mockito.RETURNS_DEEP_STUBS);
        }
    }

    private User testUser;
    private String jwtToken;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context)
                .apply(springSecurity())
                .build();

        testUser = User.builder()
                .email("chat@example.com")
                .authProvider(AuthProvider.LOCAL)
                .role(Role.USER)
                .emailVerified(true)
                .build();
        userRepository.save(testUser);

        jwtToken = jwtService.generateAccessToken(testUser);
    }

    @AfterEach
    void tearDown() {
        chatHistoryRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    void testChatEndpoint_SavesHistoryAndReturnsResponse() throws Exception {
        ChatRequest request = new ChatRequest();
        request.setMessage("What is AAPL?");

        // Mock fluent ChatClient response
        Generation generation = new Generation(new AssistantMessage("AAPL is Apple Inc."));
        ChatResponse chatResponse = new ChatResponse(List.of(generation));
        when(chatClient.prompt().messages(any(List.class)).call().chatResponse())
                .thenReturn(chatResponse);

        mockMvc.perform(MockMvcRequestBuilders.post("/api/v1/chat")
                        .header("Authorization", "Bearer " + jwtToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(MockMvcResultMatchers.status().isOk())
                .andExpect(MockMvcResultMatchers.jsonPath("$.response").value("AAPL is Apple Inc."));

        // Verify history persistence
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtAsc(testUser.getId());
        assertThat(history).hasSize(2);

        assertThat(history.get(0).getRole()).isEqualTo("USER");
        assertThat(history.get(0).getContent()).isEqualTo("What is AAPL?");

        assertThat(history.get(1).getRole()).isEqualTo("ASSISTANT");
        assertThat(history.get(1).getContent()).isEqualTo("AAPL is Apple Inc.");
    }
}
