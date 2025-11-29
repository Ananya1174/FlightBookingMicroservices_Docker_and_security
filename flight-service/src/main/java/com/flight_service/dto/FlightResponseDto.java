package com.flight_service.dto;

import com.flight_service.model.TripType;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightResponseDto {

    private Long flightId;
    private String flightNumber;
    private String airlineName;
    private String airlineLogoUrl;
    private String origin;
    private String destination;
    private OffsetDateTime departureDateTime;
    private OffsetDateTime arrivalDateTime;
    private TripType tripType;
    private BigDecimal price;
    private BigDecimal priceRoundTrip;
    private int totalSeats;
    private int availableSeats;
    private List<SeatDto> seatMap; // renamed to seatMap for clarity
}