package com.flightservice.controller;

import com.flightservice.dto.*;
import com.flightservice.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;


import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flight")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping("/airline/inventory/add")
    @PreAuthorize("hasRole('ADMIN')")

    public ResponseEntity<?> addInventory(@Valid @RequestBody FlightInventoryRequest req) {

        Long id = flightService.addInventory(req);
        URI location = URI.create(String.format("/api/flight/%d", id));

        return ResponseEntity.created(location).body(new IdResponse(id));
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> search(@Valid @RequestBody SearchRequest req) {
        List<SearchResultDto> results = flightService.searchFlights(req);
        return ResponseEntity.status(201).body(results);
    }

    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {

        FlightDetailDto detail = flightService.getDetailById(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        FlightInfoDto info = detail.getInfo();

        Map<String, Object> resp = new HashMap<>();
        resp.put("id", detail.getId());
        resp.put("flightNumber", info.getFlightNumber());
        resp.put("airlineName", info.getAirlineName());
        resp.put("airlineLogoUrl", info.getAirlineLogoUrl());
        resp.put("origin", info.getOrigin());
        resp.put("destination", info.getDestination());
        resp.put("departureTime", info.getDepartureTime());
        resp.put("arrivalTime", info.getArrivalTime());
        resp.put("price", info.getPrice());
        resp.put("tripType", info.getTripType());
        resp.put("totalSeats", info.getTotalSeats());
        resp.put("seats", detail.getSeats());

        return ResponseEntity.ok(resp);
    }
    @PostMapping("/{flightId}/seats/book")
    public ResponseEntity<Void> bookSeats(
            @PathVariable Long flightId,
            @RequestBody List<SeatBookingRequest> seats) {

        flightService.markSeatsBooked(flightId, seats);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{flightId}/seats/release")
    public ResponseEntity<Void> releaseSeats(
            @PathVariable Long flightId,
            @RequestBody List<String> seatNumbers) {

        flightService.markSeatsAvailable(flightId, seatNumbers);
        return ResponseEntity.ok().build();
    }
}