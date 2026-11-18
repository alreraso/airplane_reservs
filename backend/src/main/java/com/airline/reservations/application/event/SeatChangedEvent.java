package com.airline.reservations.application.event;

import com.airline.reservations.domain.SeatStatus;
import java.time.Instant;

/** Domain event: a seat changed state. heldUntil is only set while status is HELD. */
public record SeatChangedEvent(Long flightId, String seatNumber, SeatStatus status, Instant heldUntil) {
}
