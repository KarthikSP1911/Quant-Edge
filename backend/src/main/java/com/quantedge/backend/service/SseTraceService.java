package com.quantedge.backend.service;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseTraceService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();

    public SseEmitter createEmitter(String sessionId) {
        // 30 min timeout for research trace
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> emitters.remove(sessionId));
        emitter.onTimeout(() -> {
            emitters.remove(sessionId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.error("SSE error for session: {}", sessionId, e);
            emitters.remove(sessionId);
        });

        return emitter;
    }

    public void sendEvent(String sessionId, String eventName, Object data) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (IOException e) {
                log.warn("Failed to send SSE event for session: {}", sessionId, e);
                emitters.remove(sessionId);
                emitter.completeWithError(e);
            }
        }
    }

    public void complete(String sessionId) {
        SseEmitter emitter = emitters.get(sessionId);
        if (emitter != null) {
            emitter.complete();
            emitters.remove(sessionId);
        }
    }
}
