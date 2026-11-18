package com.airline.reservations.application;

import com.airline.reservations.application.event.FlightChangedEvent;
import com.airline.reservations.application.exception.NotFoundException;
import com.airline.reservations.domain.Flight;
import com.airline.reservations.domain.FlightRepository;
import com.airline.reservations.domain.FlightStatus;
import com.airline.reservations.domain.Seat;
import com.airline.reservations.domain.SeatRepository;
import com.airline.reservations.domain.SeatStatus;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Queries (search, seat map, stats) and the admin status change. */
@Service
public class FlightService {

    private static final LocalDateTime FAR_FUTURE = LocalDateTime.of(2999, 1, 1, 0, 0);

    private final FlightRepository flights;
    private final SeatRepository seats;
    private final ApplicationEventPublisher events;

    public FlightService(FlightRepository flights, SeatRepository seats, ApplicationEventPublisher events) {
        this.flights = flights;
        this.seats = seats;
        this.events = events;
    }

    public record SeatStats(Long flightId, long total, long available, long held, long booked) {
    }

    @Transactional(readOnly = true)
    public List<Flight> search(String origin, String destination, LocalDate date) {
        LocalDateTime from = date != null ? date.atStartOfDay() : LocalDate.now().atStartOfDay();
        LocalDateTime to = date != null ? date.plusDays(1).atStartOfDay() : FAR_FUTURE;
        return flights.search(blankToNull(origin), blankToNull(destination), from, to);
    }

    @Transactional(readOnly = true)
    public Flight get(Long flightId) {
        return flights.findById(flightId)
                .orElseThrow(() -> new NotFoundException("Flight " + flightId + " not found"));
    }

    @Transactional(readOnly = true)
    public List<Seat> seatMap(Long flightId) {
        get(flightId);
        return seats.findByFlightIdOrderById(flightId);
    }

    @Transactional(readOnly = true)
    public SeatStats stats(Long flightId) {
        return new SeatStats(flightId,
                seats.countByFlightId(flightId),
                seats.countByFlightIdAndStatus(flightId, SeatStatus.AVAILABLE),
                seats.countByFlightIdAndStatus(flightId, SeatStatus.HELD),
                seats.countByFlightIdAndStatus(flightId, SeatStatus.BOOKED));
    }

    @Transactional
    public Flight changeStatus(Long flightId, FlightStatus status) {
        Flight flight = get(flightId);
        flight.changeStatus(status);
        flights.save(flight);
        events.publishEvent(new FlightChangedEvent(flightId, status));
        return flight;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }
}
