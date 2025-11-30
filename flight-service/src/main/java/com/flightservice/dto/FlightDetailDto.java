package com.flightservice.dto;

import java.time.LocalDateTime;
import java.util.List;

public class FlightDetailDto {
    private Long id;
    private String flightNumber;
    private String airlineName;
    private String airlineLogoUrl;
    private String origin;
    private String destination;
    private LocalDateTime departureTime;
    private LocalDateTime arrivalTime;
    private Double price;
    private String tripType;
    private Integer totalSeats;

    // seat DTO
    public static class SeatDto {
        private String seatNumber;
        private String status; // AVAILABLE / BOOKED

        public SeatDto() {}
        public SeatDto(String seatNumber, String status) {
            this.seatNumber = seatNumber;
            this.status = status;
        }
        public String getSeatNumber() { return seatNumber; }
        public void setSeatNumber(String seatNumber) { this.seatNumber = seatNumber; }
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
    }

    private List<SeatDto> seats;

    // getters / setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getFlightNumber() { return flightNumber; }
    public void setFlightNumber(String flightNumber) { this.flightNumber = flightNumber; }
    public String getAirlineName() { return airlineName; }
    public void setAirlineName(String airlineName) { this.airlineName = airlineName; }
    public String getAirlineLogoUrl() { return airlineLogoUrl; }
    public void setAirlineLogoUrl(String airlineLogoUrl) { this.airlineLogoUrl = airlineLogoUrl; }
    public String getOrigin() { return origin; }
    public void setOrigin(String origin) { this.origin = origin; }
    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }
    public LocalDateTime getDepartureTime() { return departureTime; }
    public void setDepartureTime(LocalDateTime departureTime) { this.departureTime = departureTime; }
    public LocalDateTime getArrivalTime() { return arrivalTime; }
    public void setArrivalTime(LocalDateTime arrivalTime) { this.arrivalTime = arrivalTime; }
    public Double getPrice() { return price; }
    public void setPrice(Double price) { this.price = price; }
    public String getTripType() { return tripType; }
    public void setTripType(String tripType) { this.tripType = tripType; }
    public Integer getTotalSeats() { return totalSeats; }
    public void setTotalSeats(Integer totalSeats) { this.totalSeats = totalSeats; }
    public List<SeatDto> getSeats() { return seats; }
    public void setSeats(List<SeatDto> seats) { this.seats = seats; }
}