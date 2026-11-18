package com.airline.reservations.adapter.out.realtime;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Keeps the open SSE connections and fans events out to all of them.
 * State lives in this JVM's memory, which is fine for one instance (see docs/shared/decisions.md for scaling).
 */
@Component
public class SseHub {

    private static final Logger log = LoggerFactory.getLogger(SseHub.class);

    private final Set<SseEmitter> emitters = new CopyOnWriteArraySet<>();

    public SseEmitter subscribe() {
        SseEmitter emitter = new SseEmitter(0L); // no server-side timeout; the browser reconnects if the link drops
        emitters.add(emitter);
        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(error -> emitters.remove(emitter));
        send(emitter, "hello", "connected");
        return emitter;
    }

    public void broadcast(String eventName, Object payload) {
        for (SseEmitter emitter : emitters) {
            send(emitter, eventName, payload);
        }
    }

    /** Comment-only ping so dead connections are detected and dropped. */
    @Scheduled(fixedRate = 15000)
    void heartbeat() {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().comment("ping"));
            } catch (IOException | IllegalStateException e) {
                emitters.remove(emitter);
            }
        }
    }

    private void send(SseEmitter emitter, String eventName, Object payload) {
        try {
            emitter.send(SseEmitter.event().name(eventName).data(payload));
        } catch (IOException | IllegalStateException e) {
            log.debug("Dropping SSE client: {}", e.toString());
            emitters.remove(emitter);
        }
    }

    int connectedClients() {
        return emitters.size();
    }
}
