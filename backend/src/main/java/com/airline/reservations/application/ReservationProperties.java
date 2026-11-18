package com.airline.reservations.application;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Typed configuration, bound from the "reservations.*" keys in application.properties. */
@ConfigurationProperties("reservations")
public record ReservationProperties(
        @DefaultValue("5m") Duration holdDuration,
        @DefaultValue("1000") long expirationCheckMs) {
}
