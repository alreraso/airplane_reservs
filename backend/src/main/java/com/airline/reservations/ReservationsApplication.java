package com.airline.reservations;

import com.airline.reservations.application.ReservationProperties;
import java.time.Clock;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ReservationProperties.class)
public class ReservationsApplication {

	public static void main(String[] args) {
		SpringApplication.run(ReservationsApplication.class, args);
	}

	/** Injected everywhere time matters, so tests can freeze or move it. */
	@Bean
	Clock clock() {
		return Clock.systemUTC();
	}
}
