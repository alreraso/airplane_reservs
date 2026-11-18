package com.airline.reservations.application.event;

import com.airline.reservations.domain.FlightStatus;

/** Domain event: a flight changed status (delayed, cancelled, sold out...). */
public record FlightChangedEvent(Long flightId, FlightStatus status) {
}
