package com.airline.reservations.domain;

public enum FlightStatus {
    SCHEDULED,
    DELAYED,
    CANCELLED,
    SOLD_OUT;

    /** A flight accepts new holds and bookings only in these states. */
    public boolean isBookable() {
        return this == SCHEDULED || this == DELAYED;
    }
}
