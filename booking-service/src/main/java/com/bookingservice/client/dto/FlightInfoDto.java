package com.bookingservice.client.dto;

import lombok.*;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightInfoDto {
    private String flightNumber;
    private String airlineName;
    private String airlineLogoUrl;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Double price;
    private String tripType;
    private Integer totalSeats;
}