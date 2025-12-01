package com.flightservice.service;

import com.flightservice.dto.*;
import com.flightservice.mapper.FlightMapper;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private static final String STATUS_AVAILABLE = "AVAILABLE";

    public FlightService(FlightRepository flightRepository) {
        this.flightRepository = flightRepository;
    }

    @Transactional
    public FlightResponseDto addInventory(FlightInventoryRequest request) {
        Flight flight = new Flight();
        flight.setFlightNumber(request.getFlightNumber());
        flight.setAirlineName(request.getAirlineName());
        flight.setAirlineLogoUrl(request.getAirlineLogoUrl());
        flight.setOrigin(request.getOrigin());
        flight.setDestination(request.getDestination());
        flight.setDepartureTime(request.getDepartureTime());
        flight.setArrivalTime(request.getArrivalTime());
        flight.setPrice(request.getPrice());
        flight.setTripType(request.getTripType());
        flight.setTotalSeats(request.getTotalSeats());

        // create seats
        List<FlightSeat> seats = new ArrayList<>();
        if (request.getSeatNumbers() != null && !request.getSeatNumbers().isEmpty()) {
            for (String seatNo : request.getSeatNumbers()) {
                FlightSeat seat = new FlightSeat();
                seat.setSeatNumber(seatNo);
                seat.setStatus(STATUS_AVAILABLE);
                seat.setFlight(flight);
                seats.add(seat);
            }
        } else {
            for (int i = 1; i <= Optional.ofNullable(request.getTotalSeats()).orElse(0); i++) {
                FlightSeat seat = new FlightSeat();
                seat.setSeatNumber(String.valueOf(i));
                seat.setStatus(STATUS_AVAILABLE);
                seat.setFlight(flight);
                seats.add(seat);
            }
        }
        flight.setSeats(seats);

        Flight saved = flightRepository.save(flight);

        // use mapper to build response
        return FlightMapper.toResponseDto(saved);
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
                    .toList();
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
            int available = (int) f.getSeats().stream().filter(s -> STATUS_AVAILABLE.equalsIgnoreCase(s.getStatus())).count();
            r.setSeatsAvailable(available);
            return r;
        }).toList();
    }

    @Transactional(readOnly = true)
    public FlightDetailDto getFlightDetailById(Long id) {
        return flightRepository.findById(id)
                .map(FlightMapper::toDetailDto)
                .orElse(null);
    }

    // helper methods...
}