package com.flightservice.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightResponseDto {
    private Long id;
    private FlightInfoDto info;
}