package com.quantedge.backend.service;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.ChatHistory;
import com.quantedge.backend.repository.ChatHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatHistoryService {

    private final ChatHistoryRepository chatHistoryRepository;

    public List<ChatHistory> getChatHistory(UUID userId) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtAsc(userId);
    }

    @Transactional
    public void clearHistory(UUID userId) {
        chatHistoryRepository.deleteByUserId(userId);
    }
}
