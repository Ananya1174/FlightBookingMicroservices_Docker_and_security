package com.flightservice.repository;

import com.flightservice.model.Flight;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface FlightRepository extends JpaRepository<Flight, Long> {

    List<Flight> findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
            String origin, String destination, LocalDateTime start, LocalDateTime end);

    boolean existsByFlightNumberAndOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTime(
            String flightNumber, String origin, String destination, LocalDateTime departureTime);
}