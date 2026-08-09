package com.quantedge.backend.service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Slf4j
@Service
public class SseTraceService {

    private final Map<String, SseEmitter> emitters = new ConcurrentHashMap<>();
    private final Map<String, List<SseEvent>> buffer = new ConcurrentHashMap<>();
    private final Map<String, Long> sessionCreationTimes = new ConcurrentHashMap<>();
    private final Map<String, java.util.UUID> sessionOwners = new ConcurrentHashMap<>();

    private record SseEvent(String eventName, Object data) {}

    public void initSession(String sessionId, java.util.UUID ownerId) {
        buffer.put(sessionId, new CopyOnWriteArrayList<>());
        sessionCreationTimes.put(sessionId, System.currentTimeMillis());
        sessionOwners.put(sessionId, ownerId);
    }

    public boolean isOwner(String sessionId, java.util.UUID userId) {
        java.util.UUID ownerId = sessionOwners.get(sessionId);
        return ownerId != null && ownerId.equals(userId);
    }

    public SseEmitter createEmitter(String sessionId) {
        // 30 min timeout for research trace
        SseEmitter emitter = new SseEmitter(30 * 60 * 1000L);
        emitters.put(sessionId, emitter);

        emitter.onCompletion(() -> cleanupSession(sessionId));
        emitter.onTimeout(() -> {
            cleanupSession(sessionId);
            emitter.complete();
        });
        emitter.onError(e -> {
            log.error("SSE error for session: {}", sessionId, e);
            cleanupSession(sessionId);
        });

        // Flush buffered events immediately upon client connection
        List<SseEvent> bufferedEvents = buffer.getOrDefault(sessionId, new CopyOnWriteArrayList<>());
        for (SseEvent event : bufferedEvents) {
            try {
                emitter.send(SseEmitter.event().name(event.eventName()).data(event.data()));
            } catch (IOException e) {
                log.warn("Failed to flush SSE event for session: {}", sessionId, e);
                cleanupSession(sessionId);
                emitter.completeWithError(e);
                return emitter;
            }
        }

        return emitter;
    }

    public void sendEvent(String sessionId, String eventName, Object data) {
        SseEvent event = new SseEvent(eventName, data);

        List<SseEvent> bufferedEvents = buffer.computeIfAbsent(sessionId, k -> new CopyOnWriteArrayList<>());
        bufferedEvents.add(event);

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
        }
        cleanupSession(sessionId);
    }

    private void cleanupSession(String sessionId) {
        emitters.remove(sessionId);
        buffer.remove(sessionId);
        sessionCreationTimes.remove(sessionId);
        sessionOwners.remove(sessionId);
    }

    @Scheduled(fixedRate = 60 * 60 * 1000) // Run every hour
    public void cleanupOrphanedSessions() {
        long now = System.currentTimeMillis();
        long threshold = now - (2 * 60 * 60 * 1000); // 2 hours old

        List<String> toRemove = new ArrayList<>();
        for (Map.Entry<String, Long> entry : sessionCreationTimes.entrySet()) {
            if (entry.getValue() < threshold) {
                toRemove.add(entry.getKey());
            }
        }

        for (String sessionId : toRemove) {
            log.info("Cleaning up orphaned SSE session: {}", sessionId);
            complete(sessionId);
        }
    }
}
