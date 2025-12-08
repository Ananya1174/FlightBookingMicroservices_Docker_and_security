package com.bookingservice.message;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmailMessage {
    private String to;                // user email
    private String subject;
    private String body;
    private String pnr;
    private Long flightId;
    private Integer numSeats;
    private List<String> seatNumbers;
    private Instant createdAt;
}