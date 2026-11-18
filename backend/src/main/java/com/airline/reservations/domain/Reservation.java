package com.airline.reservations.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.math.BigDecimal;
import java.time.Instant;

/**
 * The unique constraint on (flightId, seatNumber) is a second safety net:
 * even if application logic had a bug, the database refuses a double booking.
 */
@Entity
@Table(uniqueConstraints = {
        @UniqueConstraint(columnNames = {"flightId", "seatNumber"}),
        @UniqueConstraint(columnNames = {"code"})
})
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String code;

    @Column(nullable = false)
    private Long flightId;

    @Column(nullable = false, length = 4)
    private String seatNumber;

    @Column(nullable = false)
    private String passengerName;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Instant createdAt;

    protected Reservation() {
        // required by JPA
    }

    public Reservation(String code, Long flightId, String seatNumber, String passengerName,
                       String email, BigDecimal amount, Instant createdAt) {
        this.code = code;
        this.flightId = flightId;
        this.seatNumber = seatNumber;
        this.passengerName = passengerName;
        this.email = email;
        this.amount = amount;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getCode() { return code; }
    public Long getFlightId() { return flightId; }
    public String getSeatNumber() { return seatNumber; }
    public String getPassengerName() { return passengerName; }
    public String getEmail() { return email; }
    public BigDecimal getAmount() { return amount; }
    public Instant getCreatedAt() { return createdAt; }
}
