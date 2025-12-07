package com.flightservice.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.flightservice.dto.*;
import com.flightservice.service.FlightService;
import org.junit.jupiter.api.*;
import org.mockito.*;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class FlightControllerTest {

    @Mock
    FlightService flightService;

    @InjectMocks
    FlightController controller;

    MockMvc mockMvc;
    ObjectMapper mapper;

    @BeforeEach
    void init() {
        MockitoAnnotations.openMocks(this);

        // configure ObjectMapper to handle Java 8 date/time types
        mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        // use ISO-8601 strings instead of timestamps
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        // provide the mapper to MockMvc so request/response bodies use this mapper
        MappingJackson2HttpMessageConverter converter = new MappingJackson2HttpMessageConverter(mapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setMessageConverters(converter)
                .setControllerAdvice(new com.flightservice.exception.GlobalExceptionHandler())
                .build();
    }

    @Test
    void addInventory_success_returns201_and_body() throws Exception {
        FlightInventoryRequest req = new FlightInventoryRequest();
        req.setAirline("SpiceJet");
        req.setFlightNumber("SG409");
        req.setOrigin("MAA");
        req.setDestination("CCU");
        req.setDeparture(LocalDateTime.now().plusDays(2));
        req.setArrival(LocalDateTime.now().plusDays(2).plusHours(2));
        req.setTotalSeats(10);
        req.setPrice(100.0);

        when(flightService.addInventory(any())).thenReturn(123L);

        mockMvc.perform(post("/api/flight/airline/inventory/add")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(123))
                .andExpect(header().string("Location", "/api/flight/123"));

        verify(flightService).addInventory(any());
    }

    @Test
    void search_returns200_and_results() throws Exception {
        SearchRequest req = new SearchRequest();
        req.setOrigin("MAA");
        req.setDestination("CCU");
        req.setTravelDate(LocalDate.now().plusDays(2));

        SearchResultDto r = new SearchResultDto();
        r.setFlightId(1L);
        when(flightService.searchFlights(any())).thenReturn(List.of(r));

        mockMvc.perform(post("/api/flight/search")
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].flightId").value(1));
    }
}