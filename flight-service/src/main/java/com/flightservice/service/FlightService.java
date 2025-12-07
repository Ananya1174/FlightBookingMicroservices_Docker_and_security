package com.flightservice.service;

import com.flightservice.dto.*;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private static final String STATUS_AVAILABLE = "AVAILABLE";

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    /**
     * Add flight inventory.
     * Returns created flight id only.
     */
    @Transactional
    public Long addInventory(FlightInventoryRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }

        // business validation
        if (req.getArrival() == null || req.getDeparture() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departure and arrival are required");
        }
        if (!req.getArrival().isAfter(req.getDeparture())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "arrival must be after departure");
        }

        // duplicate check: flightNumber + origin + destination + departure
        boolean exists = flightRepository.existsByFlightNumberAndOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTime(
                req.getFlightNumber(), req.getOrigin(), req.getDestination(), req.getDeparture());

        if (exists) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Duplicate flight already exists");
        }

        // map to entity
        Flight flight = new Flight();
        flight.setFlightNumber(req.getFlightNumber());
        flight.setAirlineName(req.getAirline());
        flight.setAirlineLogoUrl(req.getAirlineLogoUrl());
        flight.setOrigin(req.getOrigin());
        flight.setDestination(req.getDestination());
        flight.setDepartureTime(req.getDeparture());
        flight.setArrivalTime(req.getArrival());
        flight.setTotalSeats(req.getTotalSeats());
        flight.setPrice(req.getPrice());
        // default tripType to one-way if you want or leave null
        flight.setTripType("ONEWAY");

        // create seats automatically from totalSeats
        List<FlightSeat> seats = new ArrayList<>();
        int total = Optional.ofNullable(req.getTotalSeats()).orElse(0);
        for (int i = 1; i <= total; i++) {
            FlightSeat s = new FlightSeat();
            s.setSeatNumber(String.valueOf(i));
            s.setStatus(STATUS_AVAILABLE);
            s.setFlight(flight); // associate child to parent
            seats.add(s);
        }
        flight.setSeats(seats);

        Flight saved = flightRepository.save(flight);
        return saved.getId();
    }

    @Transactional(readOnly = true)
    public List<SearchResultDto> searchFlights(SearchRequest req) {
        LocalDate date = req.getTravelDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Flight> flights = flightRepository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
                req.getOrigin(), req.getDestination(), start, end);

        if (req.getTripType() != null && !req.getTripType().isBlank()) {
            flights = flights.stream()
                    .filter(f -> req.getTripType().equalsIgnoreCase(f.getTripType()))
                    .collect(Collectors.toList());
        }

        return flights.stream().map(f -> {
            SearchResultDto r = new SearchResultDto();
            r.setFlightId(f.getId());
            r.setDepartureTime(f.getDepartureTime());
            r.setArrivalTime(f.getArrivalTime());
            r.setAirlineName(f.getAirlineName());
            r.setAirlineLogoUrl(f.getAirlineLogoUrl());
            r.setPrice(f.getPrice());
            r.setTripType(f.getTripType());
            int available = (int) Optional.ofNullable(f.getSeats()).orElse(Collections.emptyList())
                    .stream().filter(s -> STATUS_AVAILABLE.equalsIgnoreCase(s.getStatus())).count();
            r.setSeatsAvailable(available);
            return r;
        }).collect(Collectors.toList());
    }
}