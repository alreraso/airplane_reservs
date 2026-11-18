package com.airline.reservations.adapter.out.realtime;

import com.airline.reservations.application.FlightService;
import com.airline.reservations.application.event.FlightChangedEvent;
import com.airline.reservations.application.event.SeatChangedEvent;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Bridges domain events to SSE clients.
 * AFTER_COMMIT matters: clients must never be told about a change that later rolls back
 * (for example a seat claimed and then undone because the payment was declined).
 */
@Component
public class RealtimeEventPublisher {

    private final SseHub hub;
    private final FlightService flightService;

    public RealtimeEventPublisher(SseHub hub, FlightService flightService) {
        this.hub = hub;
        this.flightService = flightService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onSeatChanged(SeatChangedEvent event) {
        hub.broadcast("seat", event);
        hub.broadcast("stats", flightService.stats(event.flightId()));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void onFlightChanged(FlightChangedEvent event) {
        hub.broadcast("flight", event);
    }
}
