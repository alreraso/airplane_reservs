package com.airline.reservations.adapter.in.web;

import com.airline.reservations.adapter.in.web.dto.FlightResponse;
import com.airline.reservations.adapter.in.web.dto.HoldRequest;
import com.airline.reservations.adapter.in.web.dto.HoldResponse;
import com.airline.reservations.adapter.in.web.dto.SeatResponse;
import com.airline.reservations.adapter.in.web.dto.StatusRequest;
import com.airline.reservations.application.FlightService;
import com.airline.reservations.application.FlightService.SeatStats;
import com.airline.reservations.application.SeatBookingService;
import jakarta.validation.Valid;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/** HTTP adapter: translates requests into use-case calls. No business rules here. */
@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;
    private final SeatBookingService booking;

    public FlightController(FlightService flightService, SeatBookingService booking) {
        this.flightService = flightService;
        this.booking = booking;
    }

    @GetMapping
    public List<FlightResponse> search(@RequestParam(required = false) String origin,
                                       @RequestParam(required = false) String destination,
                                       @RequestParam(required = false)
                                       @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return flightService.search(origin, destination, date).stream()
                .map(f -> FlightResponse.of(f, flightService.stats(f.getId()).available()))
                .toList();
    }

    @GetMapping("/{flightId}")
    public FlightResponse get(@PathVariable Long flightId) {
        var flight = flightService.get(flightId);
        return FlightResponse.of(flight, flightService.stats(flightId).available());
    }

    @GetMapping("/{flightId}/seats")
    public List<SeatResponse> seats(@PathVariable Long flightId,
                                    @RequestParam(required = false) String clientId) {
        return flightService.seatMap(flightId).stream()
                .map(seat -> SeatResponse.of(seat, clientId))
                .toList();
    }

    @GetMapping("/{flightId}/stats")
    public SeatStats stats(@PathVariable Long flightId) {
        flightService.get(flightId);
        return flightService.stats(flightId);
    }

    @PostMapping("/{flightId}/seats/{seatNumber}/hold")
    public HoldResponse hold(@PathVariable Long flightId, @PathVariable String seatNumber,
                             @Valid @RequestBody HoldRequest request) {
        Instant until = booking.hold(flightId, seatNumber, request.clientId());
        return new HoldResponse(seatNumber, until);
    }

    @DeleteMapping("/{flightId}/seats/{seatNumber}/hold")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void release(@PathVariable Long flightId, @PathVariable String seatNumber,
                        @RequestParam String clientId) {
        booking.release(flightId, seatNumber, clientId);
    }

    /** Admin simulation: delay or cancel a flight and watch every client update. */
    @PatchMapping("/{flightId}/status")
    public FlightResponse changeStatus(@PathVariable Long flightId, @Valid @RequestBody StatusRequest request) {
        var flight = flightService.changeStatus(flightId, request.status());
        return FlightResponse.of(flight, flightService.stats(flightId).available());
    }
}
