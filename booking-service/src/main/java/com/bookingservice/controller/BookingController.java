package com.bookingservice.controller;

import com.bookingservice.dto.BookingRequest;
import com.bookingservice.dto.BookingResponseDto;
import com.bookingservice.service.BookingService;
import com.bookingservice.security.CustomUserDetails;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/flight")
public class BookingController {

    private final BookingService bookingService;

    public BookingController(BookingService bookingService) {
        this.bookingService = bookingService;
    }

    /**
     * Book ticket for authenticated user (ROLE_USER or ROLE_ADMIN).
     * Controller sets the user email from the authenticated principal.
     */
    @PostMapping("/booking/{flightId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<BookingResponseDto> bookTicket(
            @PathVariable("flightId") Long flightId,
            @Valid @RequestBody BookingRequest request,
            @AuthenticationPrincipal CustomUserDetails user) {

        String userEmail = user.getUsername(); // username contains email in your setup
        BookingResponseDto resp = bookingService.createBooking(request, userEmail, flightId);

        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/flight/ticket/{pnr}")
                .buildAndExpand(resp.getPnr()).toUri();

        return ResponseEntity.created(location).body(resp);
    }
    

    /**
     * Public: get booking by PNR (any authenticated user can query details).
     * If you want this restricted, add @PreAuthorize accordingly.
     */
    @GetMapping("/ticket/{pnr}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<BookingResponseDto> getByPnr(@PathVariable("pnr") String pnr) {
        BookingResponseDto dto = bookingService.getByPnr(pnr);
        return ResponseEntity.ok(dto);
    }

    /**
     * Booking history:
     * - Admins can request history for any email.
     * - Regular users can only request their own history.
     */
    @GetMapping("/booking/history/{emailId}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<List<BookingResponseDto>> history(
            @PathVariable("emailId") String emailId,
            @AuthenticationPrincipal CustomUserDetails user) {

        boolean isAdmin = user.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (!isAdmin && !user.getUsername().equalsIgnoreCase(emailId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<BookingResponseDto> list = bookingService.getHistoryByEmail(emailId);
        return ResponseEntity.ok(list);
    }

    /**
     * Cancel booking:
     * - Only the booking owner (or admin) can cancel.
     */
    @DeleteMapping("/booking/cancel/{pnr}")
    @PreAuthorize("hasAnyAuthority('ROLE_USER','ROLE_ADMIN')")
    public ResponseEntity<Map<String, Object>> cancel(
            @PathVariable("pnr") String pnr,
            @AuthenticationPrincipal CustomUserDetails user) {

        String userEmail = user.getUsername();
        BookingResponseDto dto = bookingService.cancelBooking(pnr, userEmail);

        Map<String, Object> resp = new HashMap<>();
        resp.put("message", "Booking cancelled successfully");
        resp.put("pnr", dto != null ? dto.getPnr() : pnr);
        resp.put("status", "CANCELLED");

        return ResponseEntity.ok(resp);
    }
}