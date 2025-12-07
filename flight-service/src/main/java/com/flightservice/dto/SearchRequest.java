package com.flightservice.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SearchRequest {
    @NotBlank
    private String origin;

    @NotBlank
    private String destination;

    @NotNull
    private LocalDate travelDate;

    private String tripType; // optional
}