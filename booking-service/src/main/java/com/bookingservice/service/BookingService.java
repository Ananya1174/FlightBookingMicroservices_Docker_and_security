package com.bookingservice.service;

import com.bookingservice.client.FlightClientService;
import com.bookingservice.client.dto.FlightDto;
import com.bookingservice.client.dto.SeatBookingRequest;
import com.bookingservice.dto.*;
import com.bookingservice.message.EmailPublisher;
import com.bookingservice.model.Booking;
import com.bookingservice.model.Passenger;
import com.bookingservice.repository.BookingRepository;
import com.flightapp.message.EmailMessage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
public class BookingService {

    private static final Logger log = LoggerFactory.getLogger(BookingService.class);

    private final BookingRepository bookingRepository;
    private final FlightClientService flightClientService;
    private final EmailPublisher emailPublisher;

    public BookingService(
            BookingRepository bookingRepository,
            FlightClientService flightClientService,
            EmailPublisher emailPublisher) {

        this.bookingRepository = bookingRepository;
        this.flightClientService = flightClientService;
        this.emailPublisher = emailPublisher;
    }

    // =====================================================
    // CREATE BOOKING
    // =====================================================
    @Transactional
    public BookingResponseDto createBooking(
            BookingRequest request, String headerEmail, Long flightId) {

        validateAndNormalizeRequest(request, headerEmail);

        FlightDto flight = flightClientService.getFlightById(flightId);

        ensureSeatAvailabilityOrThrow(flight, request.getNumSeats());

        List<String> requestedSeatNumbers = request.getPassengers().stream()
                .map(PersonDto::getSeatNumber)
                .map(String::trim)
                .map(String::toUpperCase)
                .toList();

        checkRequestedSeatsAgainstExistingBookings(flightId, requestedSeatNumbers);

        double totalPrice = flight.getPrice() * request.getNumSeats();

        Booking booking =
                buildBookingEntity(request, totalPrice, flightId, headerEmail);

        Booking saved = bookingRepository.save(booking);

        // 🔐 Book seats AFTER DB commit
        List<SeatBookingRequest> seatRequests =
                saved.getPassengers().stream()
                        .map(p -> {
                            SeatBookingRequest s = new SeatBookingRequest();
                            s.setSeatNumber(p.getSeatNumber());
                            s.setPassengerName(p.getPassengerName());
                            return s;
                        })
                        .toList();

        registerAfterCommit(() ->
                flightClientService.bookSeats(saved.getFlightId(), seatRequests)
        );

        publishBookingEmail(saved);
        return convertToDto(saved);
    }

    // =====================================================
    // CANCEL BOOKING (24-HOUR RULE)
    // =====================================================
    @Transactional
    public BookingResponseDto cancelBooking(String pnr, String headerEmail) {

        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "PNR not found"));

        if (!booking.getUserEmail().equalsIgnoreCase(headerEmail)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not your booking");
        }

        // ✅ 24-hour cancellation validation
        long hoursSinceBooking =
                Duration.between(booking.getCreatedAt(), Instant.now()).toHours();

        if (hoursSinceBooking > 24) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Cancellation allowed only within 24 hours of booking"
            );
        }

        int updated = bookingRepository.cancelIfActive(pnr, Instant.now());
        if (updated == 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Booking already cancelled"
            );
        }

        Booking saved = bookingRepository.findByPnr(pnr)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.INTERNAL_SERVER_ERROR,
                                "Booking missing"
                        ));

        List<String> seatNumbers = saved.getPassengers().stream()
                .map(Passenger::getSeatNumber)
                .map(String::toUpperCase)
                .toList();

        // 🔓 Release seats AFTER commit
        registerAfterCommit(() ->
                flightClientService.releaseSeats(saved.getFlightId(), seatNumbers)
        );

        publishCancelEmail(saved);
        return convertToDto(saved);
    }

    // =====================================================
    // GET BOOKING BY PNR
    // =====================================================
    @Transactional(readOnly = true)
    public BookingResponseDto getByPnr(String pnr) {

        Booking booking = bookingRepository.findByPnr(pnr)
                .orElseThrow(() ->
                        new ResponseStatusException(HttpStatus.NOT_FOUND, "PNR not found"));

        return convertToDto(booking);
    }

    // =====================================================
    // BOOKING HISTORY
    // =====================================================
    @Transactional(readOnly = true)
    public List<BookingResponseDto> getHistoryByEmail(String email) {

        return bookingRepository
                .findByUserEmailOrderByCreatedAtDesc(email)
                .stream()
                .map(this::convertToDto)
                .toList();
    }

    // =====================================================
    // HELPERS
    // =====================================================

    private void validateAndNormalizeRequest(BookingRequest request, String email) {

        if (request == null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body required");

        if (email == null || email.isBlank())
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "User email required");

        request.setUserEmail(email);

        if (request.getNumSeats() == null || request.getNumSeats() <= 0)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid seat count");

        if (request.getPassengers() == null ||
                request.getPassengers().size() != request.getNumSeats()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Passenger count must match numSeats"
            );
        }
    }

    private void ensureSeatAvailabilityOrThrow(FlightDto flight, int requested) {

        long available = flight.getSeats().stream()
                .filter(s -> "AVAILABLE".equalsIgnoreCase(s.getStatus()))
                .count();

        if (available < requested) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "Only " + available + " seats available"
            );
        }
    }

    private void checkRequestedSeatsAgainstExistingBookings(
            Long flightId, List<String> seats) {

        if (seats.isEmpty()) return;

        int conflicts =
                bookingRepository.countConflictingSeats(flightId, seats);

        if (conflicts > 0) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "One or more seats already booked"
            );
        }
    }

    private Booking buildBookingEntity(
            BookingRequest request, double price, Long flightId, String userEmail) {

        Booking booking = new Booking();
        booking.setPnr(UUID.randomUUID().toString().replace("-", "")
                .substring(0, 8).toUpperCase());
        booking.setFlightId(flightId);
        booking.setUserEmail(userEmail);
        booking.setNumSeats(request.getNumSeats());
        booking.setTotalPrice(price);
        booking.setStatus("ACTIVE");
        booking.setCreatedAt(Instant.now());

        List<Passenger> passengers =
                request.getPassengers().stream()
                        .map(p -> {
                            Passenger ps = new Passenger();
                            ps.setPassengerName(p.getName());
                            ps.setAge(p.getAge());
                            ps.setGender(p.getGender());
                            ps.setMealPreference(p.getMealPreference());
                            ps.setSeatNumber(p.getSeatNumber().toUpperCase());
                            ps.setBooking(booking);
                            return ps;
                        })
                        .toList();

        booking.setPassengers(passengers);
        return booking;
    }

    private BookingResponseDto convertToDto(Booking booking) {

        BookingResponseDto dto = new BookingResponseDto();
        dto.setPnr(booking.getPnr());
        dto.setFlightId(booking.getFlightId());
        dto.setUserEmail(booking.getUserEmail());
        dto.setNumSeats(booking.getNumSeats());
        dto.setTotalPrice(booking.getTotalPrice());
        dto.setStatus(booking.getStatus());
        dto.setCreatedAt(booking.getCreatedAt());

        dto.setPassengers(
                booking.getPassengers().stream()
                        .map(p -> PersonDto.builder()
                                .name(p.getPassengerName())
                                .age(p.getAge())
                                .gender(p.getGender())
                                .seatNumber(p.getSeatNumber())
                                .mealPreference(p.getMealPreference())
                                .build())
                        .toList()
        );

        return dto;
    }

    private void publishBookingEmail(Booking booking) {

        String body = """
            Dear Customer,

            We are pleased to inform you that your flight booking has been successfully confirmed.

            Booking Details:
            ----------------
            PNR           : %s
            Flight ID     : %d
            Number of Seats: %d
            Seat Numbers  : %s
            Total Amount  : ₹%.2f
            Booking Date  : %s

            Please keep this PNR for future reference. You may be required to present it during check-in or while contacting customer support.

            We wish you a comfortable and pleasant journey.

            Sincerely,
            Flight Booking Support Team
            """.formatted(
                booking.getPnr(),
                booking.getFlightId(),
                booking.getNumSeats(),
                booking.getPassengers()
                       .stream()
                       .map(Passenger::getSeatNumber)
                       .toList(),
                booking.getTotalPrice(),
                booking.getCreatedAt()
        );

        emailPublisher.publishBookingCreated(
            new EmailMessage(
                booking.getUserEmail(),
                "Flight Booking Confirmation – PNR " + booking.getPnr(),
                body
            )
        );
    }

    private void publishCancelEmail(Booking booking) {

        String body = """
            Dear Customer,

            This email is to confirm that your flight booking has been successfully cancelled as per your request.

            Cancellation Details:
            ---------------------
            PNR           : %s
            Flight ID     : %d
            Number of Seats: %d
            Cancelled On  : %s

            If any applicable refund is initiated, it will be processed according to the airline's cancellation policy and credited to your original payment method.

            If you have any questions or require further assistance, please contact our customer support team.

            Thank you for using our flight booking service.

            Sincerely,
            Flight Booking Support Team
            """.formatted(
                booking.getPnr(),
                booking.getFlightId(),
                booking.getNumSeats(),
                booking.getCancelledAt()
        );

        emailPublisher.publishBookingCancelled(
            new EmailMessage(
                booking.getUserEmail(),
                "Flight Booking Cancellation – PNR " + booking.getPnr(),
                body
            )
        );
    }

    private void registerAfterCommit(Runnable task) {

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(
                    new TransactionSynchronization() {
                        @Override
                        public void afterCommit() {
                            task.run();
                        }
                    }
            );
        } else {
            task.run();
        }
    }
}