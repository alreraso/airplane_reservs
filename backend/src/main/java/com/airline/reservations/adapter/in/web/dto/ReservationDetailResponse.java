package com.airline.reservations.adapter.in.web.dto;

import com.airline.reservations.application.ReservationService.ReservationDetail;
import com.airline.reservations.domain.Flight;
import com.airline.reservations.domain.FlightStatus;
import com.airline.reservations.domain.Reservation;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;

/**
 * Ticket recovered by code. The code acts as the only credential, so the email is masked
 * and card data is never part of it (it is not stored at all).
 */
public record ReservationDetailResponse(String code, String passengerName, String maskedEmail,
                                        String seatNumber, BigDecimal amount, Instant createdAt,
                                        FlightSummary flight) {

    /** Current flight data, so a cancelled flight is visible when the ticket is consulted. */
    public record FlightSummary(Long id, String flightNumber, String origin, String destination,
                                LocalDateTime departureTime, LocalDateTime arrivalTime,
                                FlightStatus status) {

        static FlightSummary of(Flight f) {
            return new FlightSummary(f.getId(), f.getFlightNumber(), f.getOrigin(), f.getDestination(),
                    f.getDepartureTime(), f.getArrivalTime(), f.getStatus());
        }
    }

    public static ReservationDetailResponse of(ReservationDetail detail) {
        Reservation r = detail.reservation();
        return new ReservationDetailResponse(r.getCode(), r.getPassengerName(), maskEmail(r.getEmail()),
                r.getSeatNumber(), r.getAmount(), r.getCreatedAt(), FlightSummary.of(detail.flight()));
    }

    /** ana@example.com becomes a**@example.com */
    static String maskEmail(String email) {
        int at = email.indexOf('@');
        if (at <= 1) {
            return "***" + email.substring(Math.max(at, 0));
        }
        return email.charAt(0) + "*".repeat(Math.min(at - 1, 5)) + email.substring(at);
    }
}
