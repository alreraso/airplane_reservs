package com.airline.reservations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.Instant;

/**
 * A seat is NOT mutated through setters in the service layer.
 * State transitions happen through atomic conditional UPDATE statements in
 * {@link SeatRepository}, so the database decides who wins a race.
 */
@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"flightId", "seatNumber"}))
public class Seat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long flightId;

    @Column(nullable = false, length = 4)
    private String seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SeatStatus status = SeatStatus.AVAILABLE;

    /** Opaque client identifier of whoever holds the seat. Never exposed in the API. */
    private String heldBy;

    private Instant heldUntil;

    protected Seat() {
        // required by JPA
    }

    public Seat(Long flightId, String seatNumber) {
        this.flightId = flightId;
        this.seatNumber = seatNumber;
    }

    public Long getId() { return id; }
    public Long getFlightId() { return flightId; }
    public String getSeatNumber() { return seatNumber; }
    public SeatStatus getStatus() { return status; }
    public String getHeldBy() { return heldBy; }
    public Instant getHeldUntil() { return heldUntil; }
}
