package com.flightservice.dto;

import lombok.*;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlightInventoryRequest {

	@NotBlank
	@JsonProperty("airline")
	private String airline;

	@JsonProperty("airlineLogoUrl")
	private String airlineLogoUrl;

	@NotBlank
	private String flightNumber;

	@NotBlank
	private String origin;

	@NotBlank
	private String destination;

	@NotNull
	@JsonProperty("departure")
	private LocalDateTime departure;

	@NotNull
	@JsonProperty("arrival")
	private LocalDateTime arrival;

	@NotNull
	@Positive
	private Integer totalSeats;

	@NotNull
	@PositiveOrZero
	private Double price;

	private String tripType;
}