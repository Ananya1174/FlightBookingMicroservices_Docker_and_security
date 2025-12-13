package com.bookingservice.client.dto;

public class SeatBookingRequest {

    private String seatNumber;
    private String passengerName;

    public SeatBookingRequest() {}

    public String getSeatNumber() {
        return seatNumber;
    }

    public void setSeatNumber(String seatNumber) {
        this.seatNumber = seatNumber;
    }

    public String getPassengerName() {
        return passengerName;
    }

    public void setPassengerName(String passengerName) {
        this.passengerName = passengerName;
    }
}