package com.airline.reservations.application.exception;

/** The seat is held by someone else or already booked. Maps to HTTP 409. */
public class SeatUnavailableException extends RuntimeException {
    public SeatUnavailableException(String seatNumber) {
        super("Seat " + seatNumber + " is not available");
    }
}
