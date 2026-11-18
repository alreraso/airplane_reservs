package com.airline.reservations.adapter.in.web.dto;

import jakarta.validation.constraints.NotBlank;

public record HoldRequest(@NotBlank String clientId) {
}
