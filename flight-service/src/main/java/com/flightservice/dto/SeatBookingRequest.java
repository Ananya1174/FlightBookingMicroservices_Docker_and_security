package com.flightservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SeatBookingRequest {
    private String seatNumber;
    private String passengerName;
}