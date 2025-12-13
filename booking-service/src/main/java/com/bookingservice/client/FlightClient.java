package com.bookingservice.client;

import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.SeatBookingRequest;
import com.bookingservice.config.FeignAuthConfig;

import java.util.List;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "flight-service",  configuration = {
        FlightClientConfig.class,
        FeignAuthConfig.class
    })
public interface FlightClient {

    @GetMapping("/api/flight/{id}")
    FlightDto getFlightById(@PathVariable("id") Long id);
    
    @PostMapping("/api/flight/{flightId}/seats/book")
    void bookSeats(
            @PathVariable("flightId") Long flightId,
            @RequestBody List<SeatBookingRequest> seats);

    @PostMapping("/api/flight/{flightId}/seats/release")
    void releaseSeats(
            @PathVariable("flightId") Long flightId,
            @RequestBody List<String> seatNumbers);
}