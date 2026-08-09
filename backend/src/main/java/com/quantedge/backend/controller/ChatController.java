package com.quantedge.backend.controller;

import com.quantedge.backend.dto.request.ChatRequest;
import com.quantedge.backend.dto.response.ChatResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/chat")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping
    public ResponseEntity<ChatResponse> chat(
            @AuthenticationPrincipal User user, @Valid @RequestBody ChatRequest request) {
        return ResponseEntity.ok(chatService.chat(user, request.getMessage()));
    }
}
