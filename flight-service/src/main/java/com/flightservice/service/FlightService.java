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
     * Add flight inventory. Validations:
     *  - required fields
     *  - arrival must be after departure
     *  - departure must not be in the past
     *  - origin != destination
     *  - totalSeats > 0, price > 0
     *  - duplicate flights prevented (flightNumber + origin + destination + departure)
     *
     * Returns created flight id only.
     */
    @Transactional
    public Long addInventory(FlightInventoryRequest req) {
        validateInventoryRequest(req);

        boolean exists = flightRepository.existsByFlightNumberAndOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTime(
                req.getFlightNumber(), req.getOrigin(), req.getDestination(), req.getDeparture());

        if (exists) {
            String msg = String.format("Flight already exists: flightNumber=%s departure=%s origin=%s destination=%s",
                    req.getFlightNumber(), req.getDeparture(), req.getOrigin(), req.getDestination());
            throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
        }

        Flight flight = mapToEntity(req);

        // create seats automatically
        int total = Optional.ofNullable(req.getTotalSeats()).orElse(0);
        List<FlightSeat> seats = new ArrayList<>(total);
        for (int i = 1; i <= total; i++) {
            FlightSeat s = new FlightSeat();
            s.setSeatNumber(String.valueOf(i));
            s.setStatus(STATUS_AVAILABLE);
            s.setFlight(flight); // establish relationship
            seats.add(s);
        }
        flight.setSeats(seats);

        Flight saved = flightRepository.save(flight);
        return saved.getId();
    }

    private void validateInventoryRequest(FlightInventoryRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (isBlank(req.getAirline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "airline is required");
        }
        if (isBlank(req.getFlightNumber())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flightNumber is required");
        }
        if (isBlank(req.getOrigin()) || isBlank(req.getDestination())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "origin and destination are required");
        }
        if (req.getDeparture() == null || req.getArrival() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departure and arrival are required");
        }
        if (!req.getArrival().isAfter(req.getDeparture())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "arrival must be after departure");
        }
        if (req.getDeparture().isBefore(LocalDateTime.now())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departure cannot be in the past");
        }
        if (req.getOrigin().equalsIgnoreCase(req.getDestination())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "origin and destination cannot be identical");
        }
        if (req.getTotalSeats() == null || req.getTotalSeats() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalSeats must be > 0");
        }
        if (req.getPrice() == null || req.getPrice() <= 0.0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be > 0");
        }
    }

    private boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }

    private Flight mapToEntity(FlightInventoryRequest req) {
        Flight f = new Flight();
        f.setAirlineName(req.getAirline());
        f.setAirlineLogoUrl(req.getAirlineLogoUrl());
        f.setFlightNumber(req.getFlightNumber());
        f.setOrigin(req.getOrigin());
        f.setDestination(req.getDestination());
        f.setDepartureTime(req.getDeparture());
        f.setArrivalTime(req.getArrival());
        f.setTotalSeats(req.getTotalSeats());
        f.setPrice(req.getPrice());
        // default trip type to ONEWAY when not provided
        f.setTripType(isBlank(req.getTripType()) ? "ONEWAY" : req.getTripType());
        return f;
    }

    /**
     * Search flights by origin/destination/travelDate (+ optional tripType)
     */
    @Transactional(readOnly = true)
    public List<SearchResultDto> searchFlights(SearchRequest req) {
        if (req == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (isBlank(req.getOrigin()) || isBlank(req.getDestination())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "origin and destination are required");
        }
        if (req.getTravelDate() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "travelDate is required");
        }

        LocalDate date = req.getTravelDate();
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        List<Flight> flights = flightRepository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
                req.getOrigin(), req.getDestination(), start, end);

        if (!isBlank(req.getTripType())) {
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