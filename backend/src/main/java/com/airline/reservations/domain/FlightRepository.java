package com.airline.reservations.domain;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface FlightRepository extends JpaRepository<Flight, Long> {

    @Query("""
            select f from Flight f
            where (:origin is null or f.origin = :origin)
              and (:destination is null or f.destination = :destination)
              and f.departureTime >= :from
              and f.departureTime < :to
            order by f.departureTime
            """)
    List<Flight> search(@Param("origin") String origin,
                        @Param("destination") String destination,
                        @Param("from") LocalDateTime from,
                        @Param("to") LocalDateTime to);
}
