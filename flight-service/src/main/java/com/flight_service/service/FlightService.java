package com.flight_service.service;

import com.flight_service.dto.AddInventoryRequest;
import com.flight_service.dto.FlightResponseDto;
import com.flight_service.dto.SearchFlightRequest;
import com.flight_service.mapper.FlightMapper;
import com.flight_service.model.FlightInventory;
import com.flight_service.model.Seat;
import com.flight_service.model.SeatClass;
import com.flight_service.repository.FlightInventoryRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.List;

@Service
public class FlightService {

    private final FlightInventoryRepository repository;

    public FlightService(FlightInventoryRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public FlightResponseDto addInventory(AddInventoryRequest request) {

        FlightInventory inventory = FlightInventory.builder()
                .flightNumber(request.getFlightNumber())
                .airlineName(request.getAirlineName())
                .airlineLogoUrl(request.getAirlineLogoUrl())
                .origin(request.getOrigin())
                .destination(request.getDestination())
                .departureDateTime(request.getDepartureDateTime())
                .arrivalDateTime(request.getArrivalDateTime())
                .tripType(request.getTripType())
                .price(request.getPrice())
                .priceRoundTrip(request.getPriceRoundTrip())
                .totalSeats(request.getTotalSeats())
                .availableSeats(request.getTotalSeats())
                .build();

        // Auto-generate seat map using simple scheme: rows of 6 (A..F)
        int total = Math.max(1, request.getTotalSeats());
        String[] letters = {"A","B","C","D","E","F"};
        for (int i = 0; i < total; i++) {
            int row = (i / letters.length) + 1;
            String seatNumber = row + letters[i % letters.length];
            Seat seat = Seat.builder()
                    .seatNumber(seatNumber)
                    .seatClass(SeatClass.ECONOMY) // default for now
                    .available(true)
                    .build();
            inventory.addSeat(seat);
        }

        FlightInventory saved = repository.save(inventory);
        return FlightMapper.toDto(saved);
    }

    public List<FlightResponseDto> searchFlights(SearchFlightRequest request) {
        LocalDate d = request.getDepartureDate();
        OffsetDateTime start = d.atStartOfDay().atOffset(ZoneOffset.UTC);
        OffsetDateTime end = d.plusDays(1).atStartOfDay().atOffset(ZoneOffset.UTC).minusNanos(1);

        List<FlightInventory> found = repository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureDateTimeBetween(
                request.getOrigin(), request.getDestination(), start, end);

        return found.stream().map(FlightMapper::toDto).toList();
    }
}