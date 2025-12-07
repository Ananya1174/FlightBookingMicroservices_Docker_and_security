package com.flightservice.mapper;

import com.flightservice.dto.*;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.*;

class FlightMapperTest {

    @Test
    void toInfoDto_and_toResponseDto_and_toDetailDto() {
        Flight f = new Flight();
        f.setId(10L);
        f.setFlightNumber("A1");
        f.setAirlineName("Air");
        f.setAirlineLogoUrl("u");
        f.setOrigin("X");
        f.setDestination("Y");
        f.setDepartureTime(LocalDateTime.now().plusDays(1));
        f.setArrivalTime(LocalDateTime.now().plusDays(1).plusHours(2));
        f.setPrice(100.0);
        f.setTripType("ONEWAY");
        FlightSeat s = new FlightSeat();
        s.setSeatNumber("1");
        s.setStatus("AVAILABLE");
        s.setFlight(f);
        f.setSeats(List.of(s));

        FlightInfoDto info = FlightMapper.toInfoDto(f);
        assertThat(info.getFlightNumber()).isEqualTo("A1");

        FlightResponseDto resp = FlightMapper.toResponseDto(f);
        assertThat(resp.getId()).isEqualTo(10L);

        FlightDetailDto detail = FlightMapper.toDetailDto(f);
        assertThat(detail.getSeats()).hasSize(1);
    }

    @Test
    void toInfoDto_null_returnsNull() {
        assertThat(FlightMapper.toInfoDto(null)).isNull();
    }
}