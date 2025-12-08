package com.flightservice.controller;

import com.flightservice.dto.*;
import com.flightservice.service.FlightService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

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

    /**
     * POST /api/flight/airline/inventory/add
     * Accepts FlightInventoryRequest, performs validations in service.
     * Returns only created id in body and Location header /api/flight/{id}
     */
    @PostMapping("/airline/inventory/add")
    public ResponseEntity<?> addInventory(
            @RequestHeader(name = "X-User-Role", required = false) String role,
            @Valid @RequestBody FlightInventoryRequest req) {

        // required role prefix in your project is "ROLE_ADMIN"
        if (role == null || !role.equals("ROLE_ADMIN")) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                                 .body(Map.of("error", "forbidden", "message", "admin role required"));
        }

        Long id = flightService.addInventory(req);
        URI location = URI.create(String.format("/api/flight/%d", id));
        return ResponseEntity.created(location).body(new IdResponse(id));
    }

    /**
     * POST /api/flight/search
     */
    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> search(@Valid @RequestBody SearchRequest req) {
        List<SearchResultDto> results = flightService.searchFlights(req);
        return ResponseEntity.status(201).body(results);
    }
    /**
     * GET /api/flight/{id}
     * Return full flight details (used by booking-service).
     */
    /**
     * GET /api/flight/{id}
     * Return flattened flight JSON (top-level fields + seats) so other services can deserialize easily.
     */
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> getById(@PathVariable Long id) {
        com.flightservice.dto.FlightDetailDto detail = flightService.getDetailById(id);
        if (detail == null) {
            return ResponseEntity.notFound().build();
        }

        com.flightservice.dto.FlightInfoDto info = detail.getInfo();

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
}