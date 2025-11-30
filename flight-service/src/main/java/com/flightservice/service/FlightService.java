package com.flightservice.service;

import com.flightservice.dto.*;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.repository.FlightRepository;
import com.flightservice.repository.FlightSeatRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightService {

    private final FlightRepository flightRepository;
    private final FlightSeatRepository seatRepository;

    public FlightService(FlightRepository flightRepository, FlightSeatRepository seatRepository) {
        this.flightRepository = flightRepository;
        this.seatRepository = seatRepository;
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
                seat.setStatus("AVAILABLE");
                seat.setFlight(flight);
                seats.add(seat);
            }
        } else {
            // auto-generate seat numbers 1..totalSeats as "1","2"... or "A1"
            for (int i = 1; i <= Optional.ofNullable(request.getTotalSeats()).orElse(0); i++) {
                FlightSeat seat = new FlightSeat();
                seat.setSeatNumber(String.valueOf(i));
                seat.setStatus("AVAILABLE");
                seat.setFlight(flight);
                seats.add(seat);
            }
        }
        flight.setSeats(seats);

        Flight saved = flightRepository.save(flight);

        // prepare response dto
        FlightResponseDto dto = new FlightResponseDto();
        dto.setId(saved.getId());
        dto.setFlightNumber(saved.getFlightNumber());
        dto.setAirlineName(saved.getAirlineName());
        dto.setAirlineLogoUrl(saved.getAirlineLogoUrl());
        dto.setOrigin(saved.getOrigin());
        dto.setDestination(saved.getDestination());
        dto.setDepartureTime(saved.getDepartureTime());
        dto.setArrivalTime(saved.getArrivalTime());
        dto.setPrice(saved.getPrice());
        dto.setTripType(saved.getTripType());
        dto.setTotalSeats(saved.getTotalSeats());
        return dto;
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
            int available = (int) f.getSeats().stream().filter(s -> "AVAILABLE".equalsIgnoreCase(s.getStatus())).count();
            r.setSeatsAvailable(available);
            return r;
        }).collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public FlightDetailDto getFlightDetailById(Long id) {
        return flightRepository.findById(id)
                .map(f -> {
                    FlightDetailDto dto = new FlightDetailDto();
                    dto.setId(f.getId());
                    dto.setFlightNumber(f.getFlightNumber());
                    dto.setAirlineName(f.getAirlineName());
                    dto.setAirlineLogoUrl(f.getAirlineLogoUrl());
                    dto.setOrigin(f.getOrigin());
                    dto.setDestination(f.getDestination());
                    dto.setDepartureTime(f.getDepartureTime());
                    dto.setArrivalTime(f.getArrivalTime());
                    dto.setPrice(f.getPrice());
                    dto.setTripType(f.getTripType());
                    dto.setTotalSeats(f.getTotalSeats());

                    List<FlightDetailDto.SeatDto> seats = f.getSeats() == null ? List.of()
                        : f.getSeats().stream()
                          .map(s -> new FlightDetailDto.SeatDto(s.getSeatNumber(), s.getStatus()))
                          .collect(Collectors.toList());
                    dto.setSeats(seats);
                    return dto;
                })
                .orElse(null);
    }
    // helper methods: getFlightById, reserveSeat etc. will be used by booking-service later.
}