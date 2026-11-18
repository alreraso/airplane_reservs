package com.airline.reservations.adapter.in.web.dto;

import com.airline.reservations.domain.FlightStatus;
import jakarta.validation.constraints.NotNull;

public record StatusRequest(@NotNull FlightStatus status) {
}
