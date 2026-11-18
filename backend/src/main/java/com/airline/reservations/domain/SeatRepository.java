package com.airline.reservations.domain;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Every state transition is ONE conditional UPDATE. The row count tells the caller
 * whether it won (1) or lost (0). No read-then-write, so no race window.
 */
public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByFlightIdOrderById(Long flightId);

    Optional<Seat> findByFlightIdAndSeatNumber(Long flightId, String seatNumber);

    long countByFlightIdAndStatus(Long flightId, SeatStatus status);

    long countByFlightId(Long flightId);

    /** Wins if the seat is free, if a previous hold expired, or if the same client re-holds it. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Seat s
               set s.status = :held, s.heldBy = :clientId, s.heldUntil = :until
             where s.flightId = :flightId
               and s.seatNumber = :seatNumber
               and ( s.status = :available
                  or (s.status = :held and (s.heldUntil < :now or s.heldBy = :clientId)) )
            """)
    int tryHold(@Param("flightId") Long flightId,
                @Param("seatNumber") String seatNumber,
                @Param("clientId") String clientId,
                @Param("until") Instant until,
                @Param("now") Instant now,
                @Param("held") SeatStatus held,
                @Param("available") SeatStatus available);

    /** Wins only for the current, non-expired holder. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Seat s
               set s.status = :booked, s.heldUntil = null
             where s.flightId = :flightId
               and s.seatNumber = :seatNumber
               and s.status = :held
               and s.heldBy = :clientId
               and s.heldUntil >= :now
            """)
    int tryConfirm(@Param("flightId") Long flightId,
                   @Param("seatNumber") String seatNumber,
                   @Param("clientId") String clientId,
                   @Param("now") Instant now,
                   @Param("held") SeatStatus held,
                   @Param("booked") SeatStatus booked);

    /** Voluntary release by the holder. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Seat s
               set s.status = :available, s.heldBy = null, s.heldUntil = null
             where s.flightId = :flightId
               and s.seatNumber = :seatNumber
               and s.status = :held
               and s.heldBy = :clientId
            """)
    int tryRelease(@Param("flightId") Long flightId,
                   @Param("seatNumber") String seatNumber,
                   @Param("clientId") String clientId,
                   @Param("held") SeatStatus held,
                   @Param("available") SeatStatus available);

    @Query("select s from Seat s where s.status = :held and s.heldUntil < :now")
    List<Seat> findExpiredHolds(@Param("now") Instant now, @Param("held") SeatStatus held);

    /** Per-seat guarded expiry, so a concurrent confirm or re-hold is never overwritten. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Seat s
               set s.status = :available, s.heldBy = null, s.heldUntil = null
             where s.id = :id
               and s.status = :held
               and s.heldUntil < :now
            """)
    int tryExpire(@Param("id") Long id,
                  @Param("now") Instant now,
                  @Param("held") SeatStatus held,
                  @Param("available") SeatStatus available);
}
