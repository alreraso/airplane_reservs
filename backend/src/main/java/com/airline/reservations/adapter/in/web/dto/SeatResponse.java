package com.airline.reservations.adapter.in.web.dto;

import com.airline.reservations.domain.Seat;
import com.airline.reservations.domain.SeatStatus;
import java.time.Instant;

/** heldBy is never exposed; the caller only learns whether the hold is its own. */
public record SeatResponse(String seatNumber, SeatStatus status, Instant heldUntil, boolean heldByMe) {

    public static SeatResponse of(Seat seat, String clientId) {
        boolean mine = clientId != null && seat.getStatus() == SeatStatus.HELD
                && clientId.equals(seat.getHeldBy());
        return new SeatResponse(seat.getSeatNumber(), seat.getStatus(), seat.getHeldUntil(), mine);
    }
}
