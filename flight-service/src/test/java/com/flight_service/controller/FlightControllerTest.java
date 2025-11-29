package com.flight_service.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flight_service.dto.AddInventoryRequest;
import com.flight_service.dto.FlightResponseDto;
import com.flight_service.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = FlightController.class)
class FlightControllerTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @MockBean
    private FlightService service;

    @Test
    void addInventory_endpoint_shouldReturn201() throws Exception {
        AddInventoryRequest req = AddInventoryRequest.builder()
                .flightNumber("AI-101")
                .airlineName("Air India")
                .origin("HYD")
                .destination("BOM")
                .departureDateTime(OffsetDateTime.parse("2025-12-01T06:30:00Z"))
                .arrivalDateTime(OffsetDateTime.parse("2025-12-01T08:15:00Z"))
                .tripType(com.flight_service.model.TripType.ONE_WAY)
                .price(BigDecimal.valueOf(4500))
                .totalSeats(2)
                .build();

        FlightResponseDto resp = FlightResponseDto.builder()
                .flightId(1L)
                .flightNumber("AI-101")
                .totalSeats(2)
                .availableSeats(2)
                .seatMap(List.of())
                .build();

        when(service.addInventory(any(AddInventoryRequest.class))).thenReturn(resp);

        mvc.perform(post("/api/flights/inventory/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.flightId").value(1));
        verify(service, times(1)).addInventory(any(AddInventoryRequest.class));
    }
}