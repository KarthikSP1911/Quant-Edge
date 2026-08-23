package com.quantedge.backend.controller;

import java.util.Map;
import java.util.UUID;

import com.quantedge.backend.dto.request.AgentRunRequest;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.SseTraceService;
import com.quantedge.backend.service.agent.ResearchAgentOrchestrator;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/v1/agent")
@RequiredArgsConstructor
public class ResearchAgentController {

    private final ResearchAgentOrchestrator researchAgentOrchestrator;
    private final SseTraceService sseTraceService;

    @PostMapping("/research/{symbol}")
    public ResponseEntity<Map<String, String>> triggerResearch(
            @AuthenticationPrincipal User user,
            @PathVariable String symbol,
            @RequestBody(required = false) AgentRunRequest body) {
        String sessionId = UUID.randomUUID().toString();
        String goal =
                body != null && body.objective() != null && !body.objective().isBlank()
                        ? body.objective()
                        : "Produce a research report for " + symbol;
        Thread.ofVirtual().start(() -> researchAgentOrchestrator.runResearch(user, symbol, goal, sessionId));
        return ResponseEntity.ok(Map.of("sessionId", sessionId));
    }

    @GetMapping(value = "/trace/{sessionId}", produces = "text/event-stream")
    public SseEmitter connectTrace(@AuthenticationPrincipal User user, @PathVariable String sessionId) {
        if (!sseTraceService.isOwner(sessionId, user.getId())) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "Not authorized for this trace session");
        }
        return sseTraceService.createEmitter(sessionId);
    }
}
