package com.flightservice.dto;

import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

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
    // json property name "departure" expected in request body
    @JsonProperty("departure")
    private LocalDateTime departure;

    @NotNull
    // json property name "arrival" expected in request body
    @JsonProperty("arrival")
    private LocalDateTime arrival;

    @NotNull
    @Positive
    private Integer totalSeats;

    @NotNull
    @PositiveOrZero
    private Double price;

    // getters / setters
    public String getAirline() { return airline; }
    public void setAirline(String airline) { this.airline = airline; }

    public String getAirlineLogoUrl() { return airlineLogoUrl; }
    public void setAirlineLogoUrl(String airlineLogoUrl) { this.airlineLogoUrl = airlineLogoUrl; }

    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }

    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public LocalDateTime getDeparture() { return departure; }
    public void setDeparture(LocalDateTime departure) { this.departure = departure; }

    public LocalDateTime getArrival() { return arrival; }
    public void setArrival(LocalDateTime arrival) { this.arrival = arrival; }

    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }

    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
}