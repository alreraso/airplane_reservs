package com.airline.reservations.application;

import com.airline.reservations.application.exception.NotFoundException;
import com.airline.reservations.domain.Flight;
import com.airline.reservations.domain.FlightRepository;
import com.airline.reservations.domain.Reservation;
import com.airline.reservations.domain.ReservationRepository;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Read side of reservations: recover a ticket from its code. */
@Service
public class ReservationService {

    private final ReservationRepository reservations;
    private final FlightRepository flights;

    public ReservationService(ReservationRepository reservations, FlightRepository flights) {
        this.reservations = reservations;
        this.flights = flights;
    }

    /** A reservation together with the flight it belongs to, as it is right now. */
    public record ReservationDetail(Reservation reservation, Flight flight) {
    }

    /** The code is case-insensitive and tolerates surrounding spaces (it is usually pasted). */
    @Transactional(readOnly = true)
    public ReservationDetail findByCode(String rawCode) {
        String code = rawCode.trim().toUpperCase(Locale.ROOT);
        Reservation reservation = reservations.findByCode(code)
                .orElseThrow(() -> new NotFoundException("Reservation " + code + " not found"));
        Flight flight = flights.findById(reservation.getFlightId())
                .orElseThrow(() -> new NotFoundException("Flight of reservation " + code + " not found"));
        return new ReservationDetail(reservation, flight);
    }
}
