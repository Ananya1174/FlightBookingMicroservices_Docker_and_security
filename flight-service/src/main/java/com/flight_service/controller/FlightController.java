package com.flight_service.controller;

import com.flight_service.dto.AddInventoryRequest;
import com.flight_service.dto.FlightResponseDto;
import com.flight_service.dto.SearchFlightRequest;
import com.flight_service.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * REST controller for flight inventory operations.
 */
@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService service;

    public FlightController(FlightService service) {
        this.service = service;
    }

    /**
     * Add flight inventory. Returns created inventory including generated flightId.
     */
    @PostMapping("/inventory/add")
    public ResponseEntity<FlightResponseDto> addInventory(@Valid @RequestBody AddInventoryRequest request) {
        FlightResponseDto response = service.addInventory(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * Search flights by origin/destination and departure date.
     * Example body: { "origin": "HYD", "destination": "BOM", "departureDate": "2025-12-01", "tripType":"ONE_WAY", "passengers": 1 }
     */
    @PostMapping("/search")
    public ResponseEntity<List<FlightResponseDto>> search(@Valid @RequestBody SearchFlightRequest request) {
        List<FlightResponseDto> results = service.searchFlights(request);
        return ResponseEntity.ok(results);
    }
}
