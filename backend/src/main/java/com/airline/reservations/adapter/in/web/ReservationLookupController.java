package com.airline.reservations.adapter.in.web;

import com.airline.reservations.adapter.in.web.dto.ReservationDetailResponse;
import com.airline.reservations.application.ReservationService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reservations")
public class ReservationLookupController {

    private final ReservationService reservations;

    public ReservationLookupController(ReservationService reservations) {
        this.reservations = reservations;
    }

    @GetMapping("/{code}")
    public ReservationDetailResponse getByCode(@PathVariable String code) {
        return ReservationDetailResponse.of(reservations.findByCode(code));
    }
}
