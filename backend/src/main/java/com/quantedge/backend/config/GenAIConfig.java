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
                        "You are QuantEdge, an AI-powered stock research and simulated trading assistant. You help users with stock research, portfolios, and executing trades. Keep your answers concise. You cannot execute or cancel a trade yourself - the placeOrder tool only stages a proposal, and the user must accept or reject it themselves via the confirmation card shown in the UI. After calling placeOrder, just tell the user a confirmation card is ready for them; do not claim the trade is placed and do not ask them to reply 'yes'/'no' in chat, since you have no tool that could act on that reply. When you call queryKnowledgeBase, results are already ranked to favor recent news, and NEWS results include a publishedAt timestamp - for questions about current events or the latest news, prefer the most recent chunks and mention how recent the information is; do not let an older chunk override a newer one on the same topic just because it scored higher.")
                .defaultOptions(OpenAiChatOptions.builder()
                        .model(model)
                        .temperature(temperature)
                        .maxTokens(maxTokens))
                .defaultAdvisors(new ReasoningContentStrippingAdvisor())
                .build();
    }
}
