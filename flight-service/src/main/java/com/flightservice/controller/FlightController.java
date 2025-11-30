package com.flightservice.controller;

import com.flightservice.dto.FlightInventoryRequest;
import com.flightservice.dto.FlightResponseDto;
import com.flightservice.dto.SearchRequest;
import com.flightservice.dto.SearchResultDto;
import com.flightservice.service.FlightService;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.flightservice.dto.FlightDetailDto;

import java.util.List;

@RestController
@RequestMapping("/api/flights")
public class FlightController {

    private final FlightService flightService;

    public FlightController(FlightService flightService) {
        this.flightService = flightService;
    }

    @PostMapping("/inventory")
    public ResponseEntity<FlightResponseDto> addInventory(@Valid @RequestBody FlightInventoryRequest request) {
        FlightResponseDto dto = flightService.addInventory(request);
        return ResponseEntity.status(201).body(dto);
    }

    @PostMapping("/search")
    public ResponseEntity<List<SearchResultDto>> searchFlights(@Valid @RequestBody SearchRequest req) {
        List<SearchResultDto> results = flightService.searchFlights(req);
        return ResponseEntity.status(HttpStatus.CREATED).body(results);
    }
    @GetMapping("/{id}")
    public ResponseEntity<FlightDetailDto> getFlightById(@PathVariable("id") Long id) {
        FlightDetailDto dto = flightService.getFlightDetailById(id);
        if (dto == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(dto);
    }
}