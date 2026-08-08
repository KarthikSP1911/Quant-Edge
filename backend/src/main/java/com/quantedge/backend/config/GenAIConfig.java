package com.quantedge.backend.config;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class GenAIConfig {

    @Bean
    public ChatClient chatClient(ChatClient.Builder builder) {
        return builder.defaultSystem(
                        "You are QuantEdge, an AI-powered stock research and simulated trading assistant. You help users with stock research, portfolios, and executing trades. Keep your answers concise.")
                .build();
    }
}
