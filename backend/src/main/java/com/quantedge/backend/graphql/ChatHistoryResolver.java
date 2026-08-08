package com.quantedge.backend.graphql;

import com.quantedge.backend.entity.ChatHistory;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.repository.ChatHistoryRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatHistoryResolver {

    private final ChatHistoryRepository chatHistoryRepository;

    @QueryMapping
    public List<ChatHistory> chatHistory(@AuthenticationPrincipal User user) {
        return chatHistoryRepository.findByUserIdOrderByCreatedAtAsc(user.getId());
    }
}
