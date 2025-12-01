package com.flightservice.mapper;

import com.flightservice.model.Flight;
import com.flightservice.dto.FlightInfoDto;
import com.flightservice.dto.FlightResponseDto;
import com.flightservice.dto.FlightDetailDto;
import com.flightservice.model.FlightSeat;
import java.util.stream.Collectors;

public class FlightMapper {

    public static FlightInfoDto toInfoDto(Flight f) {
        if (f == null) return null;
        FlightInfoDto info = new FlightInfoDto();
        info.setFlightNumber(f.getFlightNumber());
        info.setAirlineName(f.getAirlineName());
        info.setAirlineLogoUrl(f.getAirlineLogoUrl());
        info.setOrigin(f.getOrigin());
        info.setDestination(f.getDestination());
        info.setDepartureTime(f.getDepartureTime());
        info.setArrivalTime(f.getArrivalTime());
        info.setPrice(f.getPrice());
        info.setTripType(f.getTripType());
        info.setTotalSeats(f.getTotalSeats());
        return info;
    }

    public static FlightResponseDto toResponseDto(Flight f) {
        FlightResponseDto dto = new FlightResponseDto();
        dto.setId(f.getId());
        dto.setInfo(toInfoDto(f));
        return dto;
    }

    public static FlightDetailDto toDetailDto(Flight f) {
        if (f == null) return null;
        FlightDetailDto dto = new FlightDetailDto();
        dto.setId(f.getId());
        dto.setInfo(toInfoDto(f));
        dto.setSeats(
            f.getSeats() == null ? null :
            f.getSeats().stream()
              .map(s -> new FlightDetailDto.SeatDto(s.getSeatNumber(), s.getStatus()))
              .toList()
        );
        return dto;
    }
}