package com.airline.reservations.application;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Releases holds whose time ran out. The interval comes from reservations.expiration-check-ms. */
@Component
public class HoldExpirationJob {

    private static final Logger log = LoggerFactory.getLogger(HoldExpirationJob.class);

    private final SeatBookingService booking;

    public HoldExpirationJob(SeatBookingService booking) {
        this.booking = booking;
    }

    @Scheduled(fixedDelayString = "${reservations.expiration-check-ms:1000}")
    public void releaseExpired() {
        int released = booking.releaseExpiredHolds();
        if (released > 0) {
            log.info("Released {} expired seat hold(s)", released);
        }
    }
}
