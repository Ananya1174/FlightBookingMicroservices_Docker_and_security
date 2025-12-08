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
}