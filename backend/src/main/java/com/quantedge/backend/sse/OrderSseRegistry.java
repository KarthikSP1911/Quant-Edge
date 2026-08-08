package com.quantedge.backend.sse;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Holds one {@link SseEmitter} per open connection, keyed by user id. A user may have several
 * emitters at once (multiple tabs/devices); all of them receive every fill event for that user.
 */
@Component
public class OrderSseRegistry {

    private static final Logger log = LoggerFactory.getLogger(OrderSseRegistry.class);
    private static final long EMITTER_TIMEOUT_MS = 30L * 60 * 1000;

    private final Map<UUID, List<SseEmitter>> emittersByUser = new ConcurrentHashMap<>();

    public SseEmitter register(UUID userId) {
        SseEmitter emitter = new SseEmitter(EMITTER_TIMEOUT_MS);
        List<SseEmitter> emitters = emittersByUser.computeIfAbsent(userId, id -> new CopyOnWriteArrayList<>());
        emitters.add(emitter);

        emitter.onCompletion(() -> remove(userId, emitter));
        emitter.onTimeout(() -> remove(userId, emitter));
        emitter.onError(ex -> remove(userId, emitter));

        return emitter;
    }

    public void sendToUser(UUID userId, String eventName, Object payload) {
        List<SseEmitter> emitters = emittersByUser.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(payload));
            } catch (Exception ex) {
                log.debug("Dropping dead SSE emitter for user={}: {}", userId, ex.toString());
                remove(userId, emitter);
            }
        }
    }

    private void remove(UUID userId, SseEmitter emitter) {
        emittersByUser.computeIfPresent(userId, (id, emitters) -> {
            emitters.remove(emitter);
            return emitters.isEmpty() ? null : emitters;
        });
    }
}
