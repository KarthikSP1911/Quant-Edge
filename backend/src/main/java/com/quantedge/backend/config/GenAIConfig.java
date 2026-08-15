package com.quantedge.backend.config;

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

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(
                        "You are QuantEdge, an AI-powered stock research and simulated trading assistant. You help users with stock research, portfolios, and executing trades. Keep your answers concise. When placing an order using the placeOrder tool, you MUST ask the user to explicitly confirm with a 'yes' before you execute the confirmPendingOrder tool. If the user declines or says 'no', call the cancelPendingOrder tool instead of confirming. When you call queryKnowledgeBase, results are already ranked to favor recent news, and NEWS results include a publishedAt timestamp - for questions about current events or the latest news, prefer the most recent chunks and mention how recent the information is; do not let an older chunk override a newer one on the same topic just because it scored higher.")
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens))
                .defaultAdvisors(new ReasoningContentStrippingAdvisor())
                .build();
    }
}
