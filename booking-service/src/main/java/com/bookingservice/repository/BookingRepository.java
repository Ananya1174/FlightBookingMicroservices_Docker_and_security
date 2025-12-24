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

	@Modifying(clearAutomatically = true, flushAutomatically = true)
	@Query("UPDATE Booking b SET b.status = 'CANCELLED', b.cancelledAt = :cancelledAt WHERE b.pnr = :pnr AND UPPER(b.status) <> 'CANCELLED'")
	int cancelIfActive(@Param("pnr") String pnr, @Param("cancelledAt") Instant cancelledAt);

	@Query("""
			SELECT COUNT(p)
			FROM Booking b
			JOIN b.passengers p
			WHERE b.flightId = :flightId
			  AND COALESCE(upper(p.seatNumber), '') IN :seatNumbers
			  AND UPPER(b.status) <> 'CANCELLED'
			""")
	int countConflictingSeats(@Param("flightId") Long flightId, @Param("seatNumbers") List<String> seatNumbers);

	@Query("""
			SELECT DISTINCT upper(p.seatNumber)
			FROM Booking b
			JOIN b.passengers p
			WHERE b.flightId = :flightId
			  AND COALESCE(upper(p.seatNumber), '') IN :seatNumbers
			  AND UPPER(b.status) <> 'CANCELLED'
			""")
	List<String> findConflictingSeatNumbers(@Param("flightId") Long flightId,
			@Param("seatNumbers") List<String> seatNumbers);
}