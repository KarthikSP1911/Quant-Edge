package com.quantedge.backend.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAIConfig {

    @Value("${quantedge.ai.groq.model}")
    private String model;

    @Value("${quantedge.ai.groq.temperature}")
    private Double temperature;

    @Value("${quantedge.ai.groq.max-tokens}")
    private Integer maxTokens;

    // Boot 4 defaults to Jackson 3 and no longer autoconfigures a classic
    // com.fasterxml.jackson.databind.ObjectMapper bean, so ChatService (which serializes tool-call
    // history with it) needs one wired explicitly.
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper().findAndRegisterModules();
    }

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(
                        "You are QuantEdge, an AI-powered stock research and simulated trading assistant. You help users with stock research, portfolios, and executing trades. Keep your answers concise. When placing an order using the placeOrder tool, you MUST ask the user to explicitly confirm with a 'yes' before you execute the confirmPendingOrder tool.")
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens))
                .build();
    }
}
