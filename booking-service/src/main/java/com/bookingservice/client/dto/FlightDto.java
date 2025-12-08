package com.bookingservice.client.dto;

import com.bookingservice.dto.SeatDto;
import com.fasterxml.jackson.annotation.JsonUnwrapped;
import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightDto {

    private Long id;

    // Tell Jackson to unwrap 'info' fields into this FlightDto so your code can call flight.getPrice()
    @JsonUnwrapped
    private FlightInfoDto info;

    // seats JSON remains the same
    private List<SeatDto> seats;

    // convenience methods (optional) so existing code keeps working
    public Double getPrice() {
        return info == null ? null : info.getPrice();
    }
    public String getFlightNumber() {
        return info == null ? null : info.getFlightNumber();
    }
    // add other convenience getters if you rely on them elsewhere
}