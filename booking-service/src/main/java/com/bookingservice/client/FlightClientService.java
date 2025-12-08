package com.bookingservice.client;

import com.bookingservice.client.dto.FlightDto;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;

@Component
public class FlightClientService {

    private static final Logger log = LoggerFactory.getLogger(FlightClientService.class);

    private final FlightClient flightClient;

    public FlightClientService(FlightClient flightClient) {
        this.flightClient = flightClient;
    }

    /**
     * Wrap the Feign call with a circuit breaker. This method is in a separate bean
     * so Spring AOP proxying works (no self-invocation problem).
     */
    @CircuitBreaker(name = "flightClient", fallbackMethod = "getFlightFallback")
    public FlightDto getFlightById(Long flightId) {
        try {
            FlightDto flight = flightClient.getFlightById(flightId);
            if (flight == null) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found: " + flightId);
            }
            return flight;
        } catch (ResponseStatusException rse) {
            // Business/4xx responses should pass through (so they are not treated as remote failures)
            throw rse;
        } catch (Exception ex) {
            log.error("Error calling flight-service for id={} : {}", flightId, ex.toString(), ex);
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                    "Error contacting flight service for flightId=" + flightId, ex);
        }
    }

    /**
     * Fallback for the circuit breaker. Signature = original params + Throwable.
     * We choose to respond with 503 (Service Unavailable) so callers can decide what to do.
     */
    public FlightDto getFlightFallback(Long flightId, Throwable t) {
        log.warn("FlightClientService.getFlightFallback invoked for id={} cause={}", flightId, t == null ? "null" : t.toString());
        throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE,
                "Flight service unavailable. Try again later.");
    }
}