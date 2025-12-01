package com.flightservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.flightservice.dto.*;
import com.flightservice.service.FlightService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FlightControllerTest {

    private MockMvc mockMvc;

    // Register JavaTimeModule automatically
    private ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Mock
    FlightService flightService;

    private FlightController controller;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
        controller = new FlightController(flightService);
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    // --------------------------------------
    // addInventory test
    // --------------------------------------
    @Test
    void addInventory_returns201_andBody() throws Exception {
        FlightInventoryRequest req = new FlightInventoryRequest();
        req.setAirlineName("Indigo");
        req.setOrigin("HYD");
        req.setDestination("BLR");
        req.setTripType("ONEWAY");
        req.setTotalSeats(2);
        req.setPrice(200.0);
        req.setDepartureTime(LocalDateTime.of(2025, 12, 10, 10, 0));
        req.setArrivalTime(LocalDateTime.of(2025, 12, 10, 12, 0));
        req.setSeatNumbers(List.of("1A", "1B"));

        // Build response using FlightInfoDto
        FlightInfoDto info = new FlightInfoDto();
        info.setAirlineName("Indigo");

        FlightResponseDto resp = new FlightResponseDto();
        resp.setId(5L);
        resp.setInfo(info);

        when(flightService.addInventory(any(FlightInventoryRequest.class))).thenReturn(resp);

        mockMvc.perform(post("/api/flights/inventory")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.airlineName").value("Indigo")); // flattened due to @JsonUnwrapped
    }

    // --------------------------------------
    // searchFlights test
    // --------------------------------------
    @Test
    void searchFlights_returns201_andList() throws Exception {
        SearchRequest req = new SearchRequest();
        req.setOrigin("HYD");
        req.setDestination("BLR");
        req.setTravelDate(LocalDate.of(2025, 12, 10));

        SearchResultDto r = new SearchResultDto();
        r.setFlightId(10L);

        when(flightService.searchFlights(any(SearchRequest.class))).thenReturn(List.of(r));

        mockMvc.perform(post("/api/flights/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$[0].flightId").value(10));
    }

    // --------------------------------------
    // getFlightById test - present
    // --------------------------------------
    @Test
    void getFlightById_returns200_whenPresent() throws Exception {

        FlightInfoDto info = new FlightInfoDto();
        info.setAirlineName("Air India");

        FlightDetailDto dto = new FlightDetailDto();
        dto.setId(99L);
        dto.setInfo(info);
        dto.setSeats(List.of()); // or null, doesn't matter for this test

        when(flightService.getFlightDetailById(99L)).thenReturn(dto);

        mockMvc.perform(get("/api/flights/99"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(99))
                .andExpect(jsonPath("$.airlineName").value("Air India"));
    }

    // --------------------------------------
    // getFlightById test - missing
    // --------------------------------------
    @Test
    void getFlightById_returns404_whenMissing() throws Exception {
        when(flightService.getFlightDetailById(123L)).thenReturn(null);

        mockMvc.perform(get("/api/flights/123"))
                .andExpect(status().isNotFound());
    }
}