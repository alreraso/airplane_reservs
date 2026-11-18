package com.airline.reservations.application.exception;

/** The payment gateway refused the charge. Maps to HTTP 402. */
public class PaymentDeclinedException extends RuntimeException {
    public PaymentDeclinedException() {
        super("Payment was declined");
    }
}
