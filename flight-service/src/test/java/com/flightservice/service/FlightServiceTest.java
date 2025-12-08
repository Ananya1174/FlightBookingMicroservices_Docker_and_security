package com.flightservice.service;

import com.flightservice.dto.*;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.repository.FlightRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FlightServiceTest {

	@Mock
	FlightRepository flightRepository;

	@InjectMocks
	FlightService flightService;

	private FlightInventoryRequest baseReq;

	@BeforeEach
	void setup() {
		baseReq = new FlightInventoryRequest();
		baseReq.setAirline("SpiceJet");
		baseReq.setAirlineLogoUrl("http://logo");
		baseReq.setFlightNumber("SG409");
		baseReq.setOrigin("MAA");
		baseReq.setDestination("CCU");
		baseReq.setDeparture(LocalDateTime.now().plusDays(2));
		baseReq.setArrival(LocalDateTime.now().plusDays(2).plusHours(2));
		baseReq.setTotalSeats(10);
		baseReq.setPrice(1000.0);
	}

	@Test
	void addInventory_happyPath_returnsId() {
		when(flightRepository.existsByFlightNumberAndOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTime(
				anyString(), anyString(), anyString(), any(LocalDateTime.class))).thenReturn(false);

		when(flightRepository.save(Mockito.any(Flight.class))).thenAnswer(inv -> {
			Flight f = inv.getArgument(0);
			f.setId(5L);
			return f;
		});

		Long id = flightService.addInventory(baseReq);
		assertThat(id).isEqualTo(5L);

		ArgumentCaptor<Flight> captor = ArgumentCaptor.forClass(Flight.class);
		verify(flightRepository).save(captor.capture());
		Flight arg = captor.getValue();
		assertThat(arg.getSeats()).hasSize(10);
		assertThat(arg.getTripType()).isEqualTo("ONEWAY");
	}

	@Test
	void addInventory_duplicate_throwsConflict() {
		when(flightRepository.existsByFlightNumberAndOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTime(
				anyString(), anyString(), anyString(), any(LocalDateTime.class))).thenReturn(true);

		ResponseStatusException ex = catchThrowableOfType(() -> flightService.addInventory(baseReq),
				ResponseStatusException.class);
		assertThat(ex).isNotNull();
		assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.CONFLICT.value());
	}

	@Test
	void addInventory_arrivalBeforeDeparture_badRequest() {
		baseReq.setArrival(baseReq.getDeparture().minusHours(1));
		ResponseStatusException ex = catchThrowableOfType(() -> flightService.addInventory(baseReq),
				ResponseStatusException.class);
		assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());

		assertThat(ex.getReason()).containsIgnoringCase("arrival must be after departure");
	}

	@Test
	void addInventory_departureInPast_badRequest() {
		baseReq.setDeparture(LocalDateTime.now().minusDays(1));
		baseReq.setArrival(LocalDateTime.now().plusDays(1));
		ResponseStatusException ex = catchThrowableOfType(() -> flightService.addInventory(baseReq),
				ResponseStatusException.class);
		assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
		assertThat(ex.getReason()).containsIgnoringCase("departure cannot be in the past");
	}

	@Test
	void addInventory_originEqualsDestination_badRequest() {
		baseReq.setDestination(baseReq.getOrigin());
		ResponseStatusException ex = catchThrowableOfType(() -> flightService.addInventory(baseReq),
				ResponseStatusException.class);
		assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void addInventory_negativeSeats_badRequest() {
		baseReq.setTotalSeats(0);
		ResponseStatusException ex = catchThrowableOfType(() -> flightService.addInventory(baseReq),
				ResponseStatusException.class);
		assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}

	@Test
	void searchFlights_filtersByTripType_and_returnsResults() {
		SearchRequest sr = new SearchRequest();
		sr.setOrigin("MAA");
		sr.setDestination("CCU");
		sr.setTravelDate(LocalDate.now().plusDays(2));
		sr.setTripType("ONEWAY");

		Flight f = new Flight();
		f.setId(11L);
		f.setDepartureTime(LocalDateTime.now().plusDays(2).withHour(6));
		f.setArrivalTime(LocalDateTime.now().plusDays(2).plusHours(2));
		f.setAirlineName("SpiceJet");
		f.setAirlineLogoUrl("u");
		f.setPrice(100.0);
		f.setTripType("ONEWAY");

		FlightSeat s = new FlightSeat();
		s.setSeatNumber("1");
		s.setStatus("AVAILABLE");
		f.setSeats(List.of(s));

		when(flightRepository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(anyString(),
				anyString(), any(LocalDateTime.class), any(LocalDateTime.class))).thenReturn(List.of(f));

		List<com.flightservice.dto.SearchResultDto> results = flightService.searchFlights(sr);
		assertThat(results).hasSize(1);
		assertThat(results.get(0).getSeatsAvailable()).isEqualTo(1);
	}

	@Test
	void searchFlights_nullRequest_badRequest() {
		ResponseStatusException ex = catchThrowableOfType(() -> flightService.searchFlights(null),
				ResponseStatusException.class);
		assertThat(ex.getStatusCode().value()).isEqualTo(HttpStatus.BAD_REQUEST.value());
	}
}