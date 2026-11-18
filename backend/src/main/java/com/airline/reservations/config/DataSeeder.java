package com.airline.reservations.config;

import com.airline.reservations.domain.Flight;
import com.airline.reservations.domain.FlightRepository;
import com.airline.reservations.domain.Seat;
import com.airline.reservations.domain.SeatRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Demo data for the in-memory database: a handful of flights with a 8x6 seat map each. */
@Component
public class DataSeeder implements ApplicationRunner {

    private static final int ROWS = 8;
    private static final String[] LETTERS = {"A", "B", "C", "D", "E", "F"};

    private final FlightRepository flights;
    private final SeatRepository seats;

    public DataSeeder(FlightRepository flights, SeatRepository seats) {
        this.flights = flights;
        this.seats = seats;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (flights.count() > 0) {
            return;
        }
        LocalDate tomorrow = LocalDate.now().plusDays(1);
        LocalDate dayAfter = LocalDate.now().plusDays(2);

        create("AV101", "BOG", "MDE", tomorrow.atTime(7, 30), 55, "189000");
        create("AV103", "BOG", "MDE", tomorrow.atTime(15, 10), 55, "210000");
        create("AV202", "MDE", "BOG", tomorrow.atTime(9, 0), 55, "175000");
        create("AV310", "BOG", "CTG", dayAfter.atTime(6, 45), 75, "260000");
        create("AV900", "BOG", "MIA", dayAfter.atTime(11, 20), 240, "1450000");
    }

    private void create(String number, String origin, String destination,
                        LocalDateTime departure, int durationMinutes, String price) {
        Flight flight = flights.save(new Flight(number, origin, destination, departure,
                departure.plusMinutes(durationMinutes), new BigDecimal(price)));

        List<Seat> map = new ArrayList<>();
        for (int row = 1; row <= ROWS; row++) {
            for (String letter : LETTERS) {
                map.add(new Seat(flight.getId(), row + letter));
            }
        }
        seats.saveAll(map);
    }
}
