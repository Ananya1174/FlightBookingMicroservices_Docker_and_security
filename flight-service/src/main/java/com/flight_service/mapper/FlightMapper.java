package com.flight_service.mapper;

import com.flight_service.dto.FlightResponseDto;
import com.flight_service.dto.SeatDto;
import com.flight_service.model.FlightInventory;
import com.flight_service.model.Seat;

import java.util.stream.Collectors;

public final class FlightMapper {

    private FlightMapper() { }

    public static FlightResponseDto toDto(FlightInventory f) {
        return FlightResponseDto.builder()
                .flightId(f.getId())
                .flightNumber(f.getFlightNumber())
                .airlineName(f.getAirlineName())
                .airlineLogoUrl(f.getAirlineLogoUrl())
                .origin(f.getOrigin())
                .destination(f.getDestination())
                .departureDateTime(f.getDepartureDateTime())
                .arrivalDateTime(f.getArrivalDateTime())
                .tripType(f.getTripType())
                .price(f.getPrice())
                .priceRoundTrip(f.getPriceRoundTrip())
                .totalSeats(f.getTotalSeats())
                .availableSeats(f.getAvailableSeats())
                .seatMap(f.getSeats().stream().map(FlightMapper::seatToDto).collect(Collectors.toList()))
                .build();
    }

    private static SeatDto seatToDto(Seat s) {
        return SeatDto.builder()
                .id(s.getId())
                .seatNumber(s.getSeatNumber())
                .seatClass(s.getSeatClass())
                .available(s.isAvailable())
                .build();
    }
}