package com.airline.reservations.application.port;

import java.math.BigDecimal;

/**
 * Outbound port. The application depends on this interface, never on a payment vendor.
 * Swapping the fake adapter for a real one does not touch the booking logic.
 */
public interface PaymentGateway {

    /** @return true if the charge was accepted */
    boolean charge(String cardNumber, BigDecimal amount);
}
