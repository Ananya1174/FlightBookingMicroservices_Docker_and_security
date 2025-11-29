package com.flight_service.dto;

import com.flight_service.model.TripType;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddInventoryRequest {

    @NotBlank
    private String flightNumber;

    @NotBlank
    private String airlineName;

    private String airlineLogoUrl;

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    @NotNull
    private OffsetDateTime departureDateTime;

    @NotNull
    private OffsetDateTime arrivalDateTime;

    @NotNull
    private TripType tripType;

    @NotNull
    @DecimalMin("0.0")
    private BigDecimal price;

    private BigDecimal priceRoundTrip;

    @Positive
    private int totalSeats; // user supplies number of seats only
}