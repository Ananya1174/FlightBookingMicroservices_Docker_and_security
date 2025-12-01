package com.flightservice.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import com.fasterxml.jackson.annotation.JsonUnwrapped;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class FlightResponseDto {
    private Long id;

    // JsonUnwrapped flattens the FlightInfoDto fields into the parent JSON
    @JsonUnwrapped
    private FlightInfoDto info;
}