package com.flightservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class FlightSeatDto {
    private String seatNumber;
    private String status;
}