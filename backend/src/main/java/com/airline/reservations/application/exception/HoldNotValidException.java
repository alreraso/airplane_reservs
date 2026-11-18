package com.airline.reservations.application.exception;

/** The caller has no active hold on the seat (never held it, or it expired). Maps to HTTP 409. */
public class HoldNotValidException extends RuntimeException {
    public HoldNotValidException(String seatNumber) {
        super("You have no active hold on seat " + seatNumber + " (it may have expired)");
    }
}
