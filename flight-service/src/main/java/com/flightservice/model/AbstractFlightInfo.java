package com.flightservice.model;

import jakarta.persistence.MappedSuperclass;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;

@MappedSuperclass
@Getter
@Setter
@NoArgsConstructor
public abstract class AbstractFlightInfo {
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