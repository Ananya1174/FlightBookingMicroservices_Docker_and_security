package com.bookingservice.repository;

import com.bookingservice.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
    Optional<Booking> findByPnr(String pnr);
    List<Booking> findByUserEmailOrderByCreatedAtDesc(String email);

    /**
     * Atomically mark booking as CANCELLED if it was not already CANCELLED.
     * Returns number of rows updated (0 = was already cancelled or not found).
     */
    @Modifying
    @Query("UPDATE Booking b SET b.status = 'CANCELLED', b.cancelledAt = :cancelledAt WHERE b.pnr = :pnr AND UPPER(b.status) <> 'CANCELLED'")
    int cancelIfActive(@Param("pnr") String pnr, @Param("cancelledAt") Instant cancelledAt);

    /**
     * Count passenger rows that conflict: same flightId, seat in provided list, and booking not cancelled.
     * Uses upper-case comparison so seat numbers are case-insensitive.
     */
    @Query("""
            SELECT COUNT(p)
            FROM Booking b
            JOIN b.passengers p
            WHERE b.flightId = :flightId
              AND COALESCE(upper(p.seatNumber), '') IN :seatNumbers
              AND UPPER(b.status) <> 'CANCELLED'
            """)
    int countConflictingSeats(@Param("flightId") Long flightId, @Param("seatNumbers") List<String> seatNumbers);

    /**
     * Return which seat numbers conflict (useful for clearer error messages).
     */
    @Query("""
            SELECT DISTINCT upper(p.seatNumber)
            FROM Booking b
            JOIN b.passengers p
            WHERE b.flightId = :flightId
              AND COALESCE(upper(p.seatNumber), '') IN :seatNumbers
              AND UPPER(b.status) <> 'CANCELLED'
            """)
    List<String> findConflictingSeatNumbers(@Param("flightId") Long flightId, @Param("seatNumbers") List<String> seatNumbers);
}