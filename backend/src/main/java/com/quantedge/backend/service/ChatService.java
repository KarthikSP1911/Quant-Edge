package com.quantedge.backend.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantedge.backend.entity.ChatHistory;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.repository.ChatHistoryRepository;
import java.util.List;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
public class ChatService {

    private final ChatClient chatClient;
    private final ChatHistoryRepository chatHistoryRepository;
    private final ObjectMapper objectMapper;

    public ChatService(ChatClient chatClient, ChatHistoryRepository chatHistoryRepository, ObjectMapper objectMapper) {
        this.chatClient = chatClient;
        this.chatHistoryRepository = chatHistoryRepository;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public com.quantedge.backend.dto.response.ChatResponse chat(User user, String userMessageText) {
        // 1. Save user message
        ChatHistory userMessage = ChatHistory.builder()
                .user(user)
                .role("USER")
                .content(userMessageText)
                .build();
        chatHistoryRepository.save(userMessage);

        // 2. Fetch history (limit to last N messages for context if needed, but here we take all)
        List<ChatHistory> history = chatHistoryRepository.findByUserIdOrderByCreatedAtAsc(user.getId());

        List<Message> messages = history.stream()
                .map(h -> {
                    if ("USER".equals(h.getRole())) {
                        return new UserMessage(h.getContent() == null ? "" : h.getContent());
                    } else {
                        return new AssistantMessage(h.getContent() == null ? "" : h.getContent());
                    }
                })
                .collect(Collectors.toList());

        // 3. Call Groq via Spring AI
        // Since we are in the initial setup, we will just pass messages to the model.
        // Tool calling will be configured in the next branch (phase-5/chat-tools), but we can enable functions here if
        // they exist.

        ChatResponse aiResponse = chatClient
                .prompt()
                .messages(messages)
                // .functions(...) // will be added in chat-tools branch
                .call()
                .chatResponse();

        Generation generation = aiResponse.getResult();
        String assistantText = generation.getOutput().getContent();

        // Convert tool calls to JSON if any exist
        String toolCallsJson = null;
        if (generation.getOutput().hasToolCalls()) {
            try {
                toolCallsJson =
                        objectMapper.writeValueAsString(generation.getOutput().getToolCalls());
            } catch (JsonProcessingException e) {
                log.warn("Failed to serialize tool calls", e);
            }
        }

        // 4. Save assistant response
        ChatHistory assistantMessage = ChatHistory.builder()
                .user(user)
                .role("ASSISTANT")
                .content(assistantText)
                .toolCalls(toolCallsJson)
                .build();
        chatHistoryRepository.save(assistantMessage);

        return com.quantedge.backend.dto.response.ChatResponse.builder()
                .response(assistantText)
                .build();
    }
}
