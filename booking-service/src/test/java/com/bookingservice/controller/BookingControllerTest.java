package com.bookingservice.controller;

import com.bookingservice.dto.*;
import com.bookingservice.exception.GlobalExceptionHandler;
import com.bookingservice.service.BookingService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests BookingController endpoints and that the GlobalExceptionHandler JSON shape is returned for error cases.
 */
@ExtendWith(SpringExtension.class)
@WebMvcTest(controllers = BookingController.class)
@Import(GlobalExceptionHandler.class)
class BookingControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private BookingService bookingService;

    @Autowired
    private ObjectMapper objectMapper;

    private BookingResponseDto sampleResponse(String pnr, Long flightId, String userEmail) {
        return BookingResponseDto.builder()
                .pnr(pnr)
                .flightId(flightId)
                .userEmail(userEmail)
                .numSeats(1)
                .totalPrice(100.0)
                .status("ACTIVE")
                .createdAt(Instant.now())
                .passengers(List.of(PersonDto.builder().name("Alice").age(30).gender("F").seatNumber("A1").build()))
                .build();
    }

    @Test
    void bookTicket_success_returnsCreatedAndLocation() throws Exception {
        BookingRequest req = BookingRequest.builder()
                .userEmail("user@example.com")
                .numSeats(1)
                .passengers(List.of(PersonDto.builder().name("Alice").age(30).gender("F").seatNumber("A1").build()))
                .build();

        BookingResponseDto resp = sampleResponse("PNRABC12", 5L, "user@example.com");
        when(bookingService.createBooking(any(BookingRequest.class), eq("user@example.com"), eq(5L))).thenReturn(resp);

        mvc.perform(post("/api/flight/booking/{flightId}", 5L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-User-Email", "user@example.com"))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("/api/flight/ticket/PNRABC12")))
                .andExpect(jsonPath("$.pnr").value("PNRABC12"))
                .andExpect(jsonPath("$.flightId").value(5))
                .andExpect(jsonPath("$.userEmail").value("user@example.com"));

        verify(bookingService, times(1)).createBooking(any(BookingRequest.class), eq("user@example.com"), eq(5L));
    }

    @Test
    void bookTicket_missingHeader_returnsBadRequest() throws Exception {
        BookingRequest req = BookingRequest.builder()
                .userEmail("user@example.com")
                .numSeats(1)
                .passengers(List.of(PersonDto.builder().name("Alice").age(30).gender("F").seatNumber("A1").build()))
                .build();

        // header missing -> service would not be called and controller should return 400 from service validation
        // But our GlobalExceptionHandler is used by controller; simulate BookingService throwing BAD_REQUEST to mirror service behavior
        when(bookingService.createBooking(any(), isNull(), anyLong()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "X-User-Email header is required"));

        mvc.perform(post("/api/flight/booking/{flightId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").exists());
    }

    @Test
    void bookTicket_invalidBody_returnsValidationErrorJson() throws Exception {
        // invalid request: numSeats null and empty passengers
        BookingRequest req = BookingRequest.builder()
                .userEmail("user@example.com")
                .numSeats(null)
                .passengers(List.of())
                .build();

        // Simulate controller calling service which will throw BAD_REQUEST for invalid body (service does validation)
        when(bookingService.createBooking(any(), eq("user@example.com"), anyLong()))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.BAD_REQUEST, "numSeats must be provided and > 0"));

        mvc.perform(post("/api/flight/booking/{flightId}", 1L)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req))
                .header("X-User-Email", "user@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("numSeats")));
    }

    @Test
    void getByPnr_success_and_notFound() throws Exception {
        BookingResponseDto resp = sampleResponse("PNR1", 2L, "a@b.com");
        when(bookingService.getByPnr("PNR1")).thenReturn(resp);

        mvc.perform(get("/api/flight/ticket/{pnr}", "PNR1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR1"))
                .andExpect(jsonPath("$.userEmail").value("a@b.com"));

        when(bookingService.getByPnr("NOTFOUND"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND, "PNR not found"));

        mvc.perform(get("/api/flight/ticket/{pnr}", "NOTFOUND"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("PNR not found")));
    }

    @Test
    void history_returnsList() throws Exception {
        BookingResponseDto r1 = sampleResponse("P1", 1L, "x@y.com");
        BookingResponseDto r2 = sampleResponse("P2", 1L, "x@y.com");
        when(bookingService.getHistoryByEmail("x@y.com")).thenReturn(List.of(r1, r2));

        mvc.perform(get("/api/flight/booking/history/{emailId}", "x@y.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].pnr").value("P1"))
                .andExpect(jsonPath("$[1].pnr").value("P2"));
    }

    @Test
    void cancel_success_and_forbidden() throws Exception {
        BookingResponseDto cancelled = sampleResponse("PNR_CANCEL", 9L, "owner@x.com");
        cancelled.setStatus("CANCELLED");

        // forbidden case: service throws 403
        when(bookingService.cancelBooking("PNRXYZ", "other@x.com"))
                .thenThrow(new org.springframework.web.server.ResponseStatusException(org.springframework.http.HttpStatus.FORBIDDEN, "Only the booking owner can cancel this booking"));

        mvc.perform(delete("/api/flight/booking/cancel/{pnr}", "PNRXYZ")
                .header("X-User-Email", "other@x.com"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));

        // success case
        when(bookingService.cancelBooking("PNR_CANCEL", "owner@x.com")).thenReturn(cancelled);

        mvc.perform(delete("/api/flight/booking/cancel/{pnr}", "PNR_CANCEL")
                .header("X-User-Email", "owner@x.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.pnr").value("PNR_CANCEL"))
                .andExpect(jsonPath("$.status").value(org.hamcrest.Matchers.notNullValue()))
                .andExpect(jsonPath("$.message").value(org.hamcrest.Matchers.containsString("cancelled")));
    }
}