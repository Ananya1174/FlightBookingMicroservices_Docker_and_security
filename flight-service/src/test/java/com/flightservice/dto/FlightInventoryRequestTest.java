package com.flightservice.dto;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

import java.time.LocalDateTime;

class FlightInventoryRequestTest {

    @Test
    void gettersAndSetters_work() {
        FlightInventoryRequest r = new FlightInventoryRequest();
        r.setAirline("X");
        r.setFlightNumber("F1");
        r.setOrigin("A");
        r.setDestination("B");
        r.setDeparture(LocalDateTime.now().plusDays(1));
        r.setArrival(LocalDateTime.now().plusDays(1).plusHours(2));
        r.setPrice(100.0);
        r.setTotalSeats(10);

        assertThat(r.getAirline()).isEqualTo("X");
        assertThat(r.getFlightNumber()).isEqualTo("F1");
        assertThat(r.getTotalSeats()).isEqualTo(10);
    }
}