package com.quantedge.backend.service;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.ChatHistory;
import com.quantedge.backend.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;

    public List<ChatHistory> getChatHistory(UUID userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }
}
