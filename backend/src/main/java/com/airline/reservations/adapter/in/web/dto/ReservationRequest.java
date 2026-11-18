package com.airline.reservations.adapter.in.web.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record ReservationRequest(
        @NotBlank String clientId,
        @NotBlank String seatNumber,
        @NotBlank String passengerName,
        @NotBlank @Email String email,
        @NotBlank @Pattern(regexp = "\\d{16}", message = "must be 16 digits") String cardNumber) {
}
