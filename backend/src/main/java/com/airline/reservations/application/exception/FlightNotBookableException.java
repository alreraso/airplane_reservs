package com.airline.reservations.application.exception;

import com.airline.reservations.domain.FlightStatus;

/** The flight is cancelled or sold out. Maps to HTTP 409. */
public class FlightNotBookableException extends RuntimeException {
    public FlightNotBookableException(FlightStatus status) {
        super("Flight is " + status + " and cannot be booked");
    }
}
