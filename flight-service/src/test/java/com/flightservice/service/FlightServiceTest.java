package com.flightservice.service;

import com.flightservice.dto.*;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.repository.FlightRepository;
import com.flightservice.repository.FlightSeatRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FlightServiceTest {

    @Mock
    FlightRepository flightRepository;

    @Mock
    FlightSeatRepository seatRepository; // not used by current FlightService but mocked

    @InjectMocks
    FlightService flightService;

    @Test
    void addInventory_withExplicitSeatNumbers_createsFlightAndSeats() {
        FlightInventoryRequest req = new FlightInventoryRequest();
        req.setAirlineName("Indigo");
        req.setOrigin("HYD");
        req.setDestination("BLR");
        req.setTripType("ONEWAY");
        req.setTotalSeats(2);
        req.setPrice(100.0);
        req.setDepartureTime(LocalDateTime.of(2025,12,10,10,0));
        req.setArrivalTime(LocalDateTime.of(2025,12,10,12,0));
        req.setSeatNumbers(List.of("1A","1B"));

        Flight saved = new Flight();
        saved.setId(42L);
        saved.setAirlineName(req.getAirlineName());
        saved.setOrigin(req.getOrigin());
        saved.setDestination(req.getDestination());
        saved.setTripType(req.getTripType());
        saved.setPrice(req.getPrice());
        saved.setDepartureTime(req.getDepartureTime());
        saved.setArrivalTime(req.getArrivalTime());
        saved.setTotalSeats(req.getTotalSeats());

        // when repository.save(...) is called, return the saved object (simulate DB)
        when(flightRepository.save(any(Flight.class))).thenAnswer(invocation -> {
            Flight f = invocation.getArgument(0);
            f.setId(42L);
            return f;
        });

        FlightResponseDto dto = flightService.addInventory(req);

        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(42L);
        // access nested FlightInfoDto
        assertThat(dto.getInfo()).isNotNull();
        assertThat(dto.getInfo().getAirlineName()).isEqualTo("Indigo");
        assertThat(dto.getInfo().getTotalSeats()).isEqualTo(2);

        // verify repository.save called once and saved.flight had seat list of size 2
        ArgumentCaptor<Flight> cap = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository, times(1)).save(cap.capture());
        Flight persisted = cap.getValue();
        assertThat(persisted.getSeats()).hasSize(2);
        assertThat(persisted.getSeats()).extracting("seatNumber").containsExactlyInAnyOrder("1A","1B");
    }

    @Test
    void addInventory_whenNoSeatNumbers_autogeneratesSeats() {
        FlightInventoryRequest req = new FlightInventoryRequest();
        req.setAirlineName("Air India");
        req.setOrigin("DEL");
        req.setDestination("MAA");
        req.setTripType("ONEWAY");
        req.setTotalSeats(3);
        req.setPrice(150.0);
        req.setDepartureTime(LocalDateTime.of(2025,12,11,8,0));
        req.setArrivalTime(LocalDateTime.of(2025,12,11,10,0));

        when(flightRepository.save(any(Flight.class))).thenAnswer(inv -> {
            Flight f = inv.getArgument(0);
            f.setId(7L);
            return f;
        });

        FlightResponseDto dto = flightService.addInventory(req);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(7L);
        // repository saved should include seats equal to totalSeats
        ArgumentCaptor<Flight> cap = ArgumentCaptor.forClass(Flight.class);
        verify(flightRepository).save(cap.capture());
        assertThat(cap.getValue().getSeats()).hasSize(3);
    }

    @Test
    void searchFlights_filtersByTripType_andCountsAvailableSeats() {
        // Prepare a flight with 2 seats: one AVAILABLE one BOOKED
        Flight f = new Flight();
        f.setId(11L);
        f.setOrigin("HYD");
        f.setDestination("BLR");
        f.setTripType("ONEWAY");
        f.setDepartureTime(LocalDateTime.of(2025,12,10,9,0));
        f.setArrivalTime(LocalDateTime.of(2025,12,10,11,0));
        FlightSeat s1 = new FlightSeat();
        s1.setStatus("AVAILABLE");
        FlightSeat s2 = new FlightSeat();
        s2.setStatus("BOOKED");
        f.setSeats(List.of(s1,s2));

        LocalDate date = LocalDate.of(2025,12,10);
        LocalDateTime start = date.atStartOfDay();
        LocalDateTime end = date.atTime(LocalTime.MAX);

        when(flightRepository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
                "HYD", "BLR", start, end))
                .thenReturn(List.of(f));

        SearchRequest req = new SearchRequest();
        req.setOrigin("HYD");
        req.setDestination("BLR");
        req.setTravelDate(date);
        req.setTripType("ONEWAY");

        List<SearchResultDto> results = flightService.searchFlights(req);
        assertThat(results).hasSize(1);
        SearchResultDto r = results.get(0);
        assertThat(r.getFlightId()).isEqualTo(11L);
        assertThat(r.getSeatsAvailable()).isEqualTo(1);
    }

    @Test
    void getFlightDetailById_returnsDto_whenPresent() {
        Flight f = new Flight();
        f.setId(100L);
        f.setFlightNumber("AI101");
        f.setAirlineName("Air India");
        f.setOrigin("HYD");
        f.setDestination("DEL");
        f.setDepartureTime(LocalDateTime.of(2025,12,12,6,0));
        f.setArrivalTime(LocalDateTime.of(2025,12,12,8,0));
        FlightSeat seat = new FlightSeat();
        seat.setSeatNumber("1");
        seat.setStatus("AVAILABLE");
        f.setSeats(List.of(seat));

        when(flightRepository.findById(100L)).thenReturn(Optional.of(f));

        FlightDetailDto dto = flightService.getFlightDetailById(100L);
        assertThat(dto).isNotNull();
        assertThat(dto.getId()).isEqualTo(100L);
        assertThat(dto.getSeats()).hasSize(1);
        assertThat(dto.getSeats().get(0).getSeatNumber()).isEqualTo("1");

        // also verify nested info values if needed
        assertThat(dto.getInfo()).isNotNull();
        assertThat(dto.getInfo().getFlightNumber()).isEqualTo("AI101");
        assertThat(dto.getInfo().getAirlineName()).isEqualTo("Air India");
    }

    @Test
    void getFlightDetailById_returnsNull_whenMissing() {
        when(flightRepository.findById(999L)).thenReturn(Optional.empty());
        FlightDetailDto dto = flightService.getFlightDetailById(999L);
        assertThat(dto).isNull();
    }
}