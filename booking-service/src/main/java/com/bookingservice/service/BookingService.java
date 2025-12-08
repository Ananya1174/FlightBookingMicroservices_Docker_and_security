package com.bookingservice.service;

import com.bookingservice.client.FlightClientService;
import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.dto.BookingRequest;
import com.bookingservice.dto.BookingResponseDto;
import com.bookingservice.dto.PersonDto;
import com.bookingservice.dto.SeatDto;
import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;
import com.bookingservice.repository.BookingRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final FlightClientService flightClientService;

    public BookingService(BookingRepository bookingRepository,
                          FlightClientService flightClientService) {
        this.bookingRepository = bookingRepository;
        this.flightClientService = flightClientService;
    }

    /**
     * Create a booking.
     * flightId comes from the URL (controller path variable) and is passed here.
     *
     * IMPORTANT: No circuit-breaker on this method — protecting only the remote call avoids
     * turning business errors (4xx) into fallback 503s.
     */
    @Transactional
    public BookingResponseDto createBooking(BookingRequest request, String headerEmail, Long flightId) {
        log.debug("createBooking called: flightId={}, headerEmail={}, numSeats={}",
                flightId, headerEmail,
                request == null ? null : request.getNumSeats());

        validateAndNormalizeRequest(request, headerEmail);

        // call wrapped flight client (circuit-breaker lives in FlightClientService)
        FlightDto flight = flightClientService.getFlightById(flightId);

        // Validate seat availability on flight (flight-level availability)
        long availableSeats = ensureSeatAvailabilityOrThrow(flight, request.getNumSeats());

        // Validate requested seat numbers shape & that they exist / are available in flight seats
        validateRequestedSeatNumbers(flight, request);

        // Build normalized requested seat list (trim + uppercase) for DB conflict check
        List<String> requestedSeatsUpper = request.getPassengers().stream()
                .map(PersonDto::getSeatNumber)
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        // Check DB for already-booked seats for this flight (atomic-ish check: repository query)
        checkRequestedSeatsAgainstExistingBookings(flightId, requestedSeatsUpper);

        // total price calculation
        double totalPrice = calculateTotalPrice(flight.getPrice(), request.getNumSeats());

        // persist booking (passenger seat numbers will be normalized before saving)
        Booking booking = buildBookingEntity(request, totalPrice, flightId);
        Booking saved = persistBookingOrThrow(booking);

        log.info("Booking saved: pnr={}, flightId={}, user={}",
                saved.getPnr(), saved.getFlightId(), saved.getUserEmail());

        return convertToDto(saved);
    }

    /**
     * Validate request body + header. This method DOES NOT expect flightId inside the body.
     */
    private void validateAndNormalizeRequest(BookingRequest request, String headerEmail) {
        if (request == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
        }
        if (headerEmail == null || headerEmail.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "X-User-Email header is required");
        }

        if (request.getUserEmail() == null || request.getUserEmail().isBlank()) {
            request.setUserEmail(headerEmail);
        } else if (!headerEmail.equalsIgnoreCase(request.getUserEmail())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Header user email must match request userEmail");
        }

        if (request.getNumSeats() == null || request.getNumSeats() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "numSeats must be provided and > 0");
        }

        if (request.getPassengers() == null || request.getPassengers().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "passengers list is required and cannot be empty");
        }
        if (request.getPassengers().size() != request.getNumSeats()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "number of passengers must match numSeats");
        }

        for (int i = 0; i < request.getPassengers().size(); i++) {
            PersonDto p = request.getPassengers().get(i);
            if (p.getName() == null || p.getName().isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger name is required for passenger index " + i);
            }
            if (p.getAge() == null || p.getAge() <= 0) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passenger age must be > 0 for passenger " + p.getName());
            }
        }
    }

    private long ensureSeatAvailabilityOrThrow(FlightDto flight, Integer requestedSeats) {
        long availableSeats = Optional.ofNullable(flight.getSeats()).orElse(Collections.emptyList())
                .stream()
                .filter(s -> s.getStatus() != null && "AVAILABLE".equalsIgnoreCase(s.getStatus()))
                .count();

        if (availableSeats < requestedSeats) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Not enough seats available: requested=" + requestedSeats + ", available=" + availableSeats);
        }
        return availableSeats;
    }

    private void validateRequestedSeatNumbers(FlightDto flight, BookingRequest request) {
        List<PersonDto> passengers = request.getPassengers();
        List<String> requestedSeats = passengers.stream()
                .map(PersonDto::getSeatNumber)
                .filter(s -> s != null && !s.isBlank())
                .map(String::toUpperCase)
                .toList();

        if (requestedSeats.isEmpty()) {
            return;
        }

        Set<String> duplicates = requestedSeats.stream()
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()))
                .entrySet().stream()
                .filter(e -> e.getValue() > 1)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());

        if (!duplicates.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Duplicate seat selection in request: " + duplicates);
        }

        Map<String, SeatDto> flightSeatMap = Optional.ofNullable(flight.getSeats()).orElse(Collections.emptyList())
                .stream()
                .filter(s -> s.getSeatNumber() != null)
                .collect(Collectors.toMap(s -> s.getSeatNumber().toUpperCase(), s -> s, (a, b) -> a));

        List<String> notFound = new ArrayList<>();
        List<String> notAvailable = new ArrayList<>();

        for (String seat : requestedSeats) {
            SeatDto seatDto = flightSeatMap.get(seat);
            if (seatDto == null) {
                notFound.add(seat);
            } else if (!"AVAILABLE".equalsIgnoreCase(Optional.ofNullable(seatDto.getStatus()).orElse(""))) {
                notAvailable.add(seat);
            }
        }

        if (!notFound.isEmpty() || !notAvailable.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            if (!notFound.isEmpty()) {
                sb.append("Requested seats not found: ").append(notFound).append(". ");
            }
            if (!notAvailable.isEmpty()) {
                sb.append("Requested seats not available: ").append(notAvailable).append(". ");
            }
            throw new ResponseStatusException(HttpStatus.CONFLICT, sb.toString().trim());
        }
    }

    private double calculateTotalPrice(Double pricePerSeat, Integer numSeats) {
        double price = (pricePerSeat == null) ? 0.0 : pricePerSeat;
        return price * numSeats;
    }

    private Booking buildBookingEntity(BookingRequest request, double totalPrice, Long flightId) {
        Booking booking = new Booking();
        booking.setPnr(generatePnr());
        booking.setFlightId(flightId);
        booking.setUserEmail(request.getUserEmail());
        booking.setNumSeats(request.getNumSeats());
        booking.setTotalPrice(totalPrice);
        booking.setStatus("ACTIVE");
        booking.setCreatedAt(Instant.now());

        // Normalize seat numbers when persisting (trim + upper) so DB stores canonical values
        List<Passenger> passengers = request.getPassengers().stream().map(pdto -> {
            Passenger p = new Passenger();
            p.setPassengerName(pdto.getName());
            p.setGender(pdto.getGender());
            p.setAge(pdto.getAge());
            String seat = pdto.getSeatNumber();
            p.setSeatNumber(seat == null ? null : seat.trim().toUpperCase());
            p.setMealPreference(pdto.getMealPreference());
            p.setBooking(booking);
            return p;
        }).toList();

        booking.setPassengers(passengers);
        return booking;
    }

    private Booking persistBookingOrThrow(Booking booking) {
        try {
            return bookingRepository.save(booking);
        } catch (Exception ex) {
            log.error("Failed to save booking to DB: {}", ex.toString(), ex);
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to save booking");
        }
    }

    @Transactional(readOnly = true)
    public BookingResponseDto getByPnr(String pnr) {
        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PNR not found"));
        return convertToDto(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponseDto> getHistoryByEmail(String email) {
        List<Booking> list = bookingRepository.findByUserEmailOrderByCreatedAtDesc(email);
        return list.stream().map(this::convertToDto).toList();
    }

    @Transactional
    public BookingResponseDto cancelBooking(String pnr, String headerEmail) {
        Booking existing = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "PNR not found"));

        if (!existing.getUserEmail().equalsIgnoreCase(headerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Only the booking owner can cancel this booking");
        }

        int updated = bookingRepository.cancelIfActive(pnr, Instant.now());
        if (updated == 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Booking already cancelled");
        }

        Booking saved = bookingRepository.findByPnr(pnr)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Booking disappeared after cancel"));

        log.info("Booking cancelled: pnr={}, flightId={}, user={}", saved.getPnr(), saved.getFlightId(), saved.getUserEmail());

        return convertToDto(saved);
    }

    /**
     * Check requested seat numbers against existing active bookings (DB).
     * Throws 409 if any conflict.
     */
    private void checkRequestedSeatsAgainstExistingBookings(Long flightId, List<String> requestedSeatsUpper) {
        if (requestedSeatsUpper == null || requestedSeatsUpper.isEmpty()) return;

        List<String> seatParam = requestedSeatsUpper.stream()
                .map(s -> s == null ? "" : s.trim().toUpperCase())
                .filter(s -> !s.isEmpty())
                .toList();

        if (seatParam.isEmpty()) return;

        int conflicts = bookingRepository.countConflictingSeats(flightId, seatParam);
        if (conflicts > 0) {
            List<String> conflicting = bookingRepository.findConflictingSeatNumbers(flightId, seatParam);
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Requested seat(s) already booked: " + conflicting);
        }
    }

    private BookingResponseDto convertToDto(Booking b) {
        BookingResponseDto dto = new BookingResponseDto();
        dto.setPnr(b.getPnr());
        dto.setFlightId(b.getFlightId());
        dto.setUserEmail(b.getUserEmail());
        dto.setNumSeats(b.getNumSeats());
        dto.setTotalPrice(b.getTotalPrice());
        dto.setStatus(b.getStatus());
        dto.setCreatedAt(b.getCreatedAt());

        List<PersonDto> pinfos = Optional.ofNullable(b.getPassengers()).orElse(Collections.emptyList())
                .stream().map(p -> PersonDto.builder()
                        .name(p.getPassengerName())
                        .gender(p.getGender())
                        .age(p.getAge())
                        .seatNumber(p.getSeatNumber())
                        .mealPreference(p.getMealPreference())
                        .build())
                .toList();

        dto.setPassengers(pinfos);
        return dto;
    }

    private String generatePnr() {
        return UUID.randomUUID()
                .toString()
                .replace("-", "")
                .substring(0, 8)
                .toUpperCase();
    }
}