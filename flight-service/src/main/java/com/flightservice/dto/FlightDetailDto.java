package com.flightservice.dto;

import java.util.List;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightDetailDto {
    private Long id;

    @JsonUnwrapped
    private FlightInfoDto info;

    private List<SeatDto> seats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatDto {
        private String seatNumber;
        private String status; // AVAILABLE / BOOKED
    }
}