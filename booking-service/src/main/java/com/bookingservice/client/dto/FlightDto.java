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

	@JsonUnwrapped
	private FlightInfoDto info;

	private List<SeatDto> seats;

	public Double getPrice() {
		return info == null ? null : info.getPrice();
	}

	public String getFlightNumber() {
		return info == null ? null : info.getFlightNumber();
	}

}