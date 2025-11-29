package com.flight_service.dto;



import com.flight_service.model.TripType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchFlightRequest {

    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    /**
     * departure date (local date) - search will find flights on that date
     */
    @NotNull
    private LocalDate departureDate;

    /**
     * For round-trip search
     */
    private LocalDate returnDate;

    @NotNull
    private TripType tripType;

}
