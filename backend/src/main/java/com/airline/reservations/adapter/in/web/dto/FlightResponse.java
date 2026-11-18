package com.airline.reservations.adapter.in.web.dto;

import com.airline.reservations.domain.Flight;
import com.airline.reservations.domain.FlightStatus;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record FlightResponse(Long id, String flightNumber, String origin, String destination,
                             LocalDateTime departureTime, LocalDateTime arrivalTime,
                             BigDecimal price, FlightStatus status, long availableSeats) {

    public static FlightResponse of(Flight f, long availableSeats) {
        return new FlightResponse(f.getId(), f.getFlightNumber(), f.getOrigin(), f.getDestination(),
                f.getDepartureTime(), f.getArrivalTime(), f.getPrice(), f.getStatus(), availableSeats);
    }
}
