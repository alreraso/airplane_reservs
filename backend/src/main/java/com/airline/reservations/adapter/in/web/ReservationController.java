package com.airline.reservations.adapter.in.web;

import com.airline.reservations.adapter.in.web.dto.ReservationRequest;
import com.airline.reservations.adapter.in.web.dto.ReservationResponse;
import com.airline.reservations.application.SeatBookingService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/flights/{flightId}/reservations")
public class ReservationController {

    private final SeatBookingService booking;

    public ReservationController(SeatBookingService booking) {
        this.booking = booking;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ReservationResponse confirm(@PathVariable Long flightId, @Valid @RequestBody ReservationRequest request) {
        var reservation = booking.confirm(flightId, request.seatNumber(), request.clientId(),
                request.passengerName(), request.email(), request.cardNumber());
        return ReservationResponse.of(reservation);
    }
}
