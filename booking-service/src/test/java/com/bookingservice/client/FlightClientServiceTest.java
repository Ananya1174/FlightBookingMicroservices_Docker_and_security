package com.bookingservice.client;

import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.FlightInfoDto;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(org.mockito.junit.jupiter.MockitoExtension.class)
class FlightClientServiceTest {

    @Mock FlightClient flightClient;
    @InjectMocks FlightClientService flightClientService;

    @Test
    void getFlightById_nullReturn_throwsNotFound() {
        when(flightClient.getFlightById(5L)).thenReturn(null);
        assertThatThrownBy(() -> flightClientService.getFlightById(5L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(404));
    }

    @Test
    void getFlightById_clientThrows_genericException_convertsTo503() {
        when(flightClient.getFlightById(7L)).thenThrow(new RuntimeException("boom"));
        assertThatThrownBy(() -> flightClientService.getFlightById(7L))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode().value()).isEqualTo(503));
    }

    @Test
    void getFlightById_success() {
        FlightInfoDto info = new FlightInfoDto();
        info.setPrice(200.0);
        FlightDto dto = new FlightDto();
        dto.setId(10L);
        dto.setInfo(info);

        when(flightClient.getFlightById(10L)).thenReturn(dto);
        var r = flightClientService.getFlightById(10L);
        assertThat(r).isNotNull();
        assertThat(r.getPrice()).isEqualTo(200.0);
    }
}