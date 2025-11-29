package com.flight_service.service;

import com.flight_service.dto.AddInventoryRequest;
import com.flight_service.dto.FlightResponseDto;
import com.flight_service.model.FlightInventory;
import com.flight_service.repository.FlightInventoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class FlightServiceTest {

    @Mock
    private FlightInventoryRepository repository;

    @InjectMocks
    private FlightService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void addInventory_shouldSaveAndReturnDto() {
        AddInventoryRequest req = AddInventoryRequest.builder()
                .flightNumber("AI-101")
                .airlineName("Air India")
                .origin("HYD")
                .destination("BOM")
                .departureDateTime(OffsetDateTime.parse("2025-12-01T06:30:00Z"))
                .arrivalDateTime(OffsetDateTime.parse("2025-12-01T08:15:00Z"))
                .tripType(com.flight_service.model.TripType.ONE_WAY)
                .price(BigDecimal.valueOf(4500))
                .totalSeats(4)
                .build();

        // stub repository
        when(repository.save(any(FlightInventory.class))).thenAnswer(inv -> {
            FlightInventory f = inv.getArgument(0);
            f.setId(1L);
            // mimic persistence-generated seat ids
            long id = 1L;
            for (var s : f.getSeats()) {
                s.setId(id++);
            }
            return f;
        });

        FlightResponseDto dto = service.addInventory(req);

        assertThat(dto).isNotNull();
        assertThat(dto.getFlightId()).isEqualTo(1L);
        assertThat(dto.getSeatMap()).hasSize(4);
        verify(repository, times(1)).save(any(FlightInventory.class));
    }

    @Test
    void addInventory_whenZeroSeats_shouldCreateAtLeastOne() {
        AddInventoryRequest req = AddInventoryRequest.builder()
                .flightNumber("AI-102")
                .airlineName("Air India")
                .origin("HYD")
                .destination("DEL")
                .departureDateTime(OffsetDateTime.now())
                .arrivalDateTime(OffsetDateTime.now().plusHours(2))
                .tripType(com.flight_service.model.TripType.ONE_WAY)
                .price(BigDecimal.valueOf(2000))
                .totalSeats(0)
                .build();

        when(repository.save(any(FlightInventory.class))).thenAnswer(inv -> {
            FlightInventory f = inv.getArgument(0);
            f.setId(2L);
            return f;
        });

        FlightResponseDto dto = service.addInventory(req);
        assertThat(dto.getTotalSeats()).isGreaterThanOrEqualTo(1);
        assertThat(dto.getSeatMap()).isNotEmpty();
    }
}