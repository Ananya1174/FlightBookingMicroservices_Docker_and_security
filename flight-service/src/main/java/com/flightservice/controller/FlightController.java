package com.flightservice.controller;

import com.flightservice.dto.*;
import com.flightservice.service.FlightService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.List;

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
    public ResponseEntity<IdResponse> addInventory(@Valid @RequestBody FlightInventoryRequest req) {
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
        return ResponseEntity.ok(results);
    }
}