package com.flight_service.repository;


import com.flight_service.model.FlightInventory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.OffsetDateTime;
import java.util.List;

@Repository
public interface FlightInventoryRepository extends JpaRepository<FlightInventory, Long> {

    List<FlightInventory> findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateTimeBetween(
            String origin,
            String destination,
            OffsetDateTime start,
            OffsetDateTime end
    );
}
