package com.bookingservice.service;

import com.bookingservice.client.FlightClientService;
import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.FlightInfoDto;
import com.bookingservice.dto.*;
import com.bookingservice.message.EmailPublisher;
import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;
import com.bookingservice.repository.BookingRepository;
import com.flightapp.message.EmailMessage;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class BookingServiceTest {

	@Mock
	BookingRepository bookingRepository;
	@Mock
	FlightClientService flightClientService;
	@Mock
	EmailPublisher emailPublisher;

	@InjectMocks
	BookingService bookingService;

	private FlightDto sampleFlight(Long flightId, int availableSeats, String seatPrefix) {
		FlightInfoDto info = FlightInfoDto.builder().flightNumber("F123").airlineName("ACME")
				.departureTime(LocalDateTime.now().plusDays(1))
				.arrivalTime(LocalDateTime.now().plusDays(1).plusHours(2)).price(100.0).totalSeats(availableSeats)
				.build();

		List<SeatDto> seats = new ArrayList<>();
		for (int i = 1; i <= availableSeats; i++) {
			SeatDto s = new SeatDto();
			s.setSeatNumber(seatPrefix + i);
			s.setStatus("AVAILABLE");
			seats.add(s);
		}
		FlightDto f = FlightDto.builder().id(flightId).info(info).seats(seats).build();
		return f;
	}

	private BookingRequest makeRequest(String userEmail, int numSeats, List<PersonDto> passengers) {
		BookingRequest req = BookingRequest.builder().userEmail(userEmail).numSeats(numSeats).passengers(passengers)
				.build();
		return req;
	}

	private PersonDto person(String name, int age, String gender, String seat) {
		return PersonDto.builder().name(name).age(age).gender(gender).seatNumber(seat).build();
	}

	@BeforeEach
	void setup() {
		// default no-op
	}

	@Test
	void createBooking_success_shouldPersistAndSendEmail() {
		Long flightId = 11L;
		FlightDto flight = sampleFlight(flightId, 5, "A");
		when(flightClientService.getFlightById(flightId)).thenReturn(flight);
		when(bookingRepository.countConflictingSeats(anyLong(), anyList())).thenReturn(0);
		when(bookingRepository.findConflictingSeatNumbers(anyLong(), anyList())).thenReturn(Collections.emptyList());

		List<PersonDto> p = List.of(person("Alice", 30, "F", "A1"), person("Bob", 28, "M", "A2"));
		BookingRequest req = makeRequest(null, 2, p);

		ArgumentCaptor<Booking> cap = ArgumentCaptor.forClass(Booking.class);
		when(bookingRepository.save(cap.capture())).thenAnswer(invocation -> {
			Booking b = cap.getValue();

			b.setId(100L);

			if (b.getPassengers() != null) {
				for (Passenger passenger : b.getPassengers())
					passenger.setBooking(b);
			}
			return b;
		});

		var dto = bookingService.createBooking(req, "user@example.com", flightId);

		assertThat(dto).isNotNull();
		assertThat(dto.getFlightId()).isEqualTo(flightId);
		assertThat(dto.getNumSeats()).isEqualTo(2);
		assertThat(dto.getUserEmail()).isEqualTo("user@example.com");
		assertThat(dto.getPassengers()).hasSize(2);
		verify(emailPublisher, times(1)).publishBookingCreated(any(EmailMessage.class));
		verify(bookingRepository, times(1)).save(any(Booking.class));
	}

	@Test
	void createBooking_nullRequest_throwsBadRequest() {
		assertThatThrownBy(() -> bookingService.createBooking(null, "u@x.com", 1L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createBooking_headerMissing_throwsBadRequest() {
		List<PersonDto> p = List.of(person("A", 20, "M", null));
		BookingRequest req = makeRequest(null, 1, p);
		assertThatThrownBy(() -> bookingService.createBooking(req, null, 1L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createBooking_headerMismatch_throwsBadRequest() {
		List<PersonDto> p = List.of(person("A", 20, "M", null));
		BookingRequest req = makeRequest("other@example.com", 1, p);
		assertThatThrownBy(() -> bookingService.createBooking(req, "user@example.com", 1L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createBooking_numSeatsMismatch_throwsBadRequest() {
		List<PersonDto> p = List.of(person("A", 20, "M", null));
		BookingRequest req = makeRequest(null, 2, p);
		assertThatThrownBy(() -> bookingService.createBooking(req, "user@example.com", 1L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createBooking_notEnoughSeats_throwsConflict() {
		FlightDto flight = sampleFlight(2L, 1, "A"); // only 1 available
		when(flightClientService.getFlightById(2L)).thenReturn(flight);

		List<PersonDto> p = List.of(person("A", 20, "M", null), person("B", 21, "F", null));
		BookingRequest req = makeRequest(null, 2, p);

		assertThatThrownBy(() -> bookingService.createBooking(req, "u@x.com", 2L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
	}

	@Test
	void createBooking_duplicateSeatsInRequest_throwsBadRequest() {
		FlightDto flight = sampleFlight(3L, 5, "A");
		when(flightClientService.getFlightById(3L)).thenReturn(flight);

		List<PersonDto> p = List.of(person("A", 20, "M", "A1"), person("B", 21, "F", "A1"));
		BookingRequest req = makeRequest(null, 2, p);

		assertThatThrownBy(() -> bookingService.createBooking(req, "u@x.com", 3L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(400));
	}

	@Test
	void createBooking_requestedSeatNotFound_throwsConflict() {

		FlightDto flight = sampleFlight(4L, 3, "A");
		when(flightClientService.getFlightById(4L)).thenReturn(flight);

		List<PersonDto> p = List.of(person("A", 20, "M", "Z1"));
		BookingRequest req = makeRequest(null, 1, p);

		assertThatThrownBy(() -> bookingService.createBooking(req, "user@example.com", 4L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
	}

	@Test
	void createBooking_requestedSeatNotAvailable_throwsConflict() {
		FlightDto flight = sampleFlight(5L, 3, "A");

		flight.getSeats().get(1).setStatus("BOOKED");
		when(flightClientService.getFlightById(5L)).thenReturn(flight);

		List<PersonDto> p = List.of(person("A", 20, "M", "A2"));
		BookingRequest req = makeRequest(null, 1, p);

		assertThatThrownBy(() -> bookingService.createBooking(req, "user@example.com", 5L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
	}

	@Test
	void createBooking_conflictingSeatsInDb_throwsConflict() {
		FlightDto flight = sampleFlight(6L, 5, "A");
		when(flightClientService.getFlightById(6L)).thenReturn(flight);
		when(bookingRepository.countConflictingSeats(eq(6L), anyList())).thenReturn(1);
		when(bookingRepository.findConflictingSeatNumbers(eq(6L), anyList())).thenReturn(List.of("A1"));

		List<PersonDto> p = List.of(person("A", 20, "M", "A1"));
		BookingRequest req = makeRequest(null, 1, p);

		assertThatThrownBy(() -> bookingService.createBooking(req, "user@example.com", 6L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));
	}

	@Test
	void createBooking_persistThrows_internalServerError() {
		FlightDto flight = sampleFlight(7L, 5, "A");
		when(flightClientService.getFlightById(7L)).thenReturn(flight);
		when(bookingRepository.countConflictingSeats(anyLong(), anyList())).thenReturn(0);
		when(bookingRepository.save(any())).thenThrow(new RuntimeException("DB down"));

		List<PersonDto> p = List.of(person("A", 20, "M", "A1"));
		BookingRequest req = makeRequest(null, 1, p);

		assertThatThrownBy(() -> bookingService.createBooking(req, "user@example.com", 7L))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(500));
	}

	@Test
	void getByPnr_success_and_notFound() {
		Booking b = new Booking();
		b.setPnr("PNR1234");
		b.setFlightId(1L);
		b.setUserEmail("u@x.com");
		b.setNumSeats(1);
		b.setStatus("ACTIVE");
		b.setPassengers(Collections.emptyList());
		when(bookingRepository.findByPnr("PNR1234")).thenReturn(Optional.of(b));

		var dto = bookingService.getByPnr("PNR1234");
		assertThat(dto).isNotNull();
		assertThat(dto.getPnr()).isEqualTo("PNR1234");

		when(bookingRepository.findByPnr("NOTFOUND")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> bookingService.getByPnr("NOTFOUND")).isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
	}

	@Test
	void getHistoryByEmail_returnsList() {
		Booking b1 = new Booking();
		b1.setPnr("P1");
		b1.setUserEmail("a@b.com");
		b1.setCreatedAt(Instant.now());
		Booking b2 = new Booking();
		b2.setPnr("P2");
		b2.setUserEmail("a@b.com");
		b2.setCreatedAt(Instant.now().minusSeconds(60));
		when(bookingRepository.findByUserEmailOrderByCreatedAtDesc("a@b.com")).thenReturn(List.of(b1, b2));

		var res = bookingService.getHistoryByEmail("a@b.com");
		assertThat(res).hasSize(2);
		assertThat(res.get(0).getPnr()).isEqualTo("P1");
	}

	@Test
	void cancelBooking_notFound_and_forbidden_and_alreadyCancelled_and_success() {
		when(bookingRepository.findByPnr("MISSING")).thenReturn(Optional.empty());
		assertThatThrownBy(() -> bookingService.cancelBooking("MISSING", "u@x.com"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));

		Booking existing = new Booking();
		existing.setPnr("PXY");
		existing.setUserEmail("owner@x.com");
		existing.setStatus("ACTIVE");
		when(bookingRepository.findByPnr("PXY")).thenReturn(Optional.of(existing));

		assertThatThrownBy(() -> bookingService.cancelBooking("PXY", "other@x.com"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(403));

		when(bookingRepository.findByPnr("PXY")).thenReturn(Optional.of(existing));

		when(bookingRepository.cancelIfActive("PXY", any())).thenReturn(0);
		assertThatThrownBy(() -> bookingService.cancelBooking("PXY", "owner@x.com"))
				.isInstanceOf(ResponseStatusException.class)
				.satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(409));

		existing.setStatus("ACTIVE");
		when(bookingRepository.findByPnr("PXY")).thenReturn(Optional.of(existing));
		when(bookingRepository.cancelIfActive("PXY", any())).thenReturn(1);
		Booking after = new Booking();
		after.setPnr("PXY");
		after.setStatus("CANCELLED");
		after.setUserEmail("owner@x.com");
		when(bookingRepository.findByPnr("PXY")).thenReturn(Optional.of(after));

		var dto = bookingService.cancelBooking("PXY", "owner@x.com");
		assertThat(dto).isNotNull();
		assertThat(dto.getStatus()).isEqualTo("CANCELLED");
		verify(emailPublisher, times(1)).publishBookingCancelled(any(EmailMessage.class));
	}
}