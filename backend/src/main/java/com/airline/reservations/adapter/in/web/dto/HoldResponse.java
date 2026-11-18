package com.airline.reservations.adapter.in.web.dto;

import java.time.Instant;

public record HoldResponse(String seatNumber, Instant heldUntil) {
}
