package com.quantedge.backend.repository;

import com.quantedge.backend.entity.ChatHistory;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChatHistoryRepository extends JpaRepository<ChatHistory, UUID> {
    List<ChatHistory> findByUserIdOrderByCreatedAtAsc(UUID userId);

    List<ChatHistory> findTop50ByUserIdOrderByCreatedAtDesc(UUID userId);
}
