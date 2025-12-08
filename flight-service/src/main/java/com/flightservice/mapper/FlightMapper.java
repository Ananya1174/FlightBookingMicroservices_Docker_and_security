package com.flightservice.mapper;

import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.dto.*;

import java.util.List;

public final class FlightMapper {

    private FlightMapper() {}

    public static FlightInfoDto toInfoDto(Flight f) {
        if (f == null) return null;
        return FlightInfoDto.builder()
                .flightNumber(f.getFlightNumber())
                .airlineName(f.getAirlineName())
                .airlineLogoUrl(f.getAirlineLogoUrl())
                .origin(f.getOrigin())
                .destination(f.getDestination())
                .departureTime(f.getDepartureTime())
                .arrivalTime(f.getArrivalTime())
                .price(f.getPrice())
                .tripType(f.getTripType())
                .totalSeats(f.getTotalSeats())
                .build();
    }

    public static FlightResponseDto toResponseDto(Flight f) {
        if (f == null) return null;
        return FlightResponseDto.builder()
                .id(f.getId())
                .info(toInfoDto(f))
                .build();
    }

    public static FlightDetailDto toDetailDto(Flight f) {
        if (f == null) return null;
        List<FlightDetailDto.SeatDto> seats = null;
        if (f.getSeats() != null) {
            seats = f.getSeats().stream()
                    .map(s -> new FlightDetailDto.SeatDto(s.getSeatNumber(), s.getStatus()))
                    .toList();
        }
        FlightDetailDto dto = new FlightDetailDto();
        dto.setId(f.getId());
        dto.setInfo(toInfoDto(f));
        dto.setSeats(seats);
        return dto;
    }

    public static FlightSeatDto toSeatDto(FlightSeat s) {
        if (s == null) return null;
        return new FlightSeatDto(s.getSeatNumber(), s.getStatus());
    }
}