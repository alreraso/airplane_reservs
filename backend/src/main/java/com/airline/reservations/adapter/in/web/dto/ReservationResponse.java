package com.airline.reservations.adapter.in.web.dto;

import com.airline.reservations.domain.Reservation;
import java.math.BigDecimal;
import java.time.Instant;

public record ReservationResponse(String code, Long flightId, String seatNumber, String passengerName,
                                  BigDecimal amount, Instant createdAt) {

    public static ReservationResponse of(Reservation r) {
        return new ReservationResponse(r.getCode(), r.getFlightId(), r.getSeatNumber(),
                r.getPassengerName(), r.getAmount(), r.getCreatedAt());
    }
}
