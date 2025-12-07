package com.flightservice.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightDetailDto {
    private Long id;
    private FlightInfoDto info;
    private List<SeatDto> seats;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SeatDto {
        private String seatNumber;
        private String status;
    }
}