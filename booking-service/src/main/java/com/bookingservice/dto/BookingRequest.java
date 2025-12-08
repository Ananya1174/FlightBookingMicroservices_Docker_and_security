package com.bookingservice.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.List;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingRequest {

    private String userEmail;

    @Positive
    private Integer numSeats;

    @NotEmpty
    @Valid
    private List<PersonDto> passengers;
}