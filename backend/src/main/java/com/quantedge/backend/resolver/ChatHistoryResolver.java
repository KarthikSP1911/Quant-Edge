package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.ChatHistoryResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.mapper.ChatHistoryMapper;
import com.quantedge.backend.service.ChatHistoryService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatHistoryResolver {

    private final ChatHistoryService chatHistoryService;
    private final ChatHistoryMapper chatHistoryMapper;

    @QueryMapping
    public List<ChatHistoryResponse> chatHistory(@AuthenticationPrincipal User user) {
        return chatHistoryService.getChatHistory(user.getId()).stream()
                .map(chatHistoryMapper::toDto)
                .collect(Collectors.toList());
    }
}
