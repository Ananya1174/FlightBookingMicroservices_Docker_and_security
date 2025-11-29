package com.flight_service.dto;

import com.flight_service.model.SeatClass;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeatDto {
    private Long id;
    private String seatNumber;
    private SeatClass seatClass;
    private boolean available;
}