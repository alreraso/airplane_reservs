package com.airline.reservations.application;

import com.airline.reservations.application.event.FlightChangedEvent;
import com.airline.reservations.application.event.SeatChangedEvent;
import com.airline.reservations.application.exception.FlightNotBookableException;
import com.airline.reservations.application.exception.HoldNotValidException;
import com.airline.reservations.application.exception.NotFoundException;
import com.airline.reservations.application.exception.PaymentDeclinedException;
import com.airline.reservations.application.exception.SeatUnavailableException;
import com.airline.reservations.application.port.PaymentGateway;
import com.airline.reservations.domain.Flight;
import com.airline.reservations.domain.FlightRepository;
import com.airline.reservations.domain.FlightStatus;
import com.airline.reservations.domain.Reservation;
import com.airline.reservations.domain.ReservationRepository;
import com.airline.reservations.domain.Seat;
import com.airline.reservations.domain.SeatRepository;
import com.airline.reservations.domain.SeatStatus;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Use cases for the seat lifecycle: hold, release, confirm, expire.
 * Concurrency is resolved by the atomic UPDATEs in {@link SeatRepository}.
 * Events are published inside the transaction; listeners fire only AFTER COMMIT.
 */
@Service
public class SeatBookingService {

    private final FlightRepository flights;
    private final SeatRepository seats;
    private final ReservationRepository reservations;
    private final PaymentGateway payments;
    private final ApplicationEventPublisher events;
    private final ReservationProperties properties;
    private final Clock clock;

    public SeatBookingService(FlightRepository flights, SeatRepository seats,
                              ReservationRepository reservations, PaymentGateway payments,
                              ApplicationEventPublisher events, ReservationProperties properties,
                              Clock clock) {
        this.flights = flights;
        this.seats = seats;
        this.reservations = reservations;
        this.payments = payments;
        this.events = events;
        this.properties = properties;
        this.clock = clock;
    }

    @Transactional
    public Instant hold(Long flightId, String seatNumber, String clientId) {
        requireBookable(flightId);
        Instant now = Instant.now(clock);
        Instant until = now.plus(properties.holdDuration());

        int updated = seats.tryHold(flightId, seatNumber, clientId, until, now,
                SeatStatus.HELD, SeatStatus.AVAILABLE);
        if (updated == 0) {
            // Either the seat does not exist or someone else won the race.
            seats.findByFlightIdAndSeatNumber(flightId, seatNumber)
                    .orElseThrow(() -> new NotFoundException("Seat " + seatNumber + " not found"));
            throw new SeatUnavailableException(seatNumber);
        }
        events.publishEvent(new SeatChangedEvent(flightId, seatNumber, SeatStatus.HELD, until));
        return until;
    }

    @Transactional
    public void release(Long flightId, String seatNumber, String clientId) {
        int updated = seats.tryRelease(flightId, seatNumber, clientId,
                SeatStatus.HELD, SeatStatus.AVAILABLE);
        if (updated == 1) {
            events.publishEvent(new SeatChangedEvent(flightId, seatNumber, SeatStatus.AVAILABLE, null));
        }
        // Releasing something you do not hold is a no-op: idempotent on purpose.
    }

    /**
     * Seat claim, payment and reservation insert share ONE transaction.
     * If the payment is declined, the exception rolls back the seat claim too.
     */
    @Transactional
    public Reservation confirm(Long flightId, String seatNumber, String clientId,
                               String passengerName, String email, String cardNumber) {
        Flight flight = requireBookable(flightId);
        Instant now = Instant.now(clock);

        int updated = seats.tryConfirm(flightId, seatNumber, clientId, now,
                SeatStatus.HELD, SeatStatus.BOOKED);
        if (updated == 0) {
            throw new HoldNotValidException(seatNumber);
        }
        if (!payments.charge(cardNumber, flight.getPrice())) {
            throw new PaymentDeclinedException();
        }

        Reservation reservation = reservations.save(new Reservation(newCode(), flightId, seatNumber,
                passengerName, email, flight.getPrice(), now));

        events.publishEvent(new SeatChangedEvent(flightId, seatNumber, SeatStatus.BOOKED, null));
        markSoldOutIfFull(flight);
        return reservation;
    }

    /** Called by the scheduled job. Returns how many holds were released. */
    @Transactional
    public int releaseExpiredHolds() {
        Instant now = Instant.now(clock);
        List<Seat> expired = seats.findExpiredHolds(now, SeatStatus.HELD);
        int released = 0;
        for (var seat : expired) {
            if (seats.tryExpire(seat.getId(), now, SeatStatus.HELD, SeatStatus.AVAILABLE) == 1) {
                events.publishEvent(new SeatChangedEvent(seat.getFlightId(), seat.getSeatNumber(),
                        SeatStatus.AVAILABLE, null));
                released++;
            }
        }
        return released;
    }

    private Flight requireBookable(Long flightId) {
        Flight flight = flights.findById(flightId)
                .orElseThrow(() -> new NotFoundException("Flight " + flightId + " not found"));
        if (!flight.getStatus().isBookable()) {
            throw new FlightNotBookableException(flight.getStatus());
        }
        return flight;
    }

    private void markSoldOutIfFull(Flight flight) {
        long total = seats.countByFlightId(flight.getId());
        long booked = seats.countByFlightIdAndStatus(flight.getId(), SeatStatus.BOOKED);
        if (total > 0 && booked == total) {
            flight.changeStatus(FlightStatus.SOLD_OUT);
            flights.save(flight);
            events.publishEvent(new FlightChangedEvent(flight.getId(), FlightStatus.SOLD_OUT));
        }
    }

    private static String newCode() {
        return "RSV-" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
    }
}
