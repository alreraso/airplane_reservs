package com.airline.reservations.adapter.in.web;

import com.airline.reservations.adapter.out.realtime.SseHub;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * One global event stream. Every event carries a flightId and the browser filters
 * what it cares about. Events: "seat", "stats", "flight".
 */
@RestController
@RequestMapping("/api/stream")
public class StreamController {

    private final SseHub hub;

    public StreamController(SseHub hub) {
        this.hub = hub;
    }

    @GetMapping(produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter stream() {
        return hub.subscribe();
    }
}
