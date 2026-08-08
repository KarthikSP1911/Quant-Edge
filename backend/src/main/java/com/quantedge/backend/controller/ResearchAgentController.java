package com.quantedge.backend.controller;

import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.ResearchAgentService;
import com.quantedge.backend.service.SseTraceService;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class ResearchAgentController {

    private final ResearchAgentService researchAgentService;
    private final SseTraceService sseTraceService;

    @PostMapping("/research/{symbol}")
    public ResponseEntity<Map<String, String>> triggerResearch(
            @AuthenticationPrincipal User user, @PathVariable String symbol) {
        String sessionId = UUID.randomUUID().toString();
        // Run agent async
        Thread.ofVirtual().start(() -> researchAgentService.runResearch(user, symbol, sessionId));
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    @GetMapping(value = "/trace/{sessionId}", produces = "text/event-stream")
    public SseEmitter connectTrace(@PathVariable String sessionId) {
        return sseTraceService.createEmitter(sessionId);
    }
}
