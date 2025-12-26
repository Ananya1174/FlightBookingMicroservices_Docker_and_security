package com.flightservice.service;

import com.flightservice.dto.*;
import com.flightservice.model.Flight;
import com.flightservice.model.FlightSeat;
import com.flightservice.repository.FlightRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class FlightService {

	private final FlightRepository flightRepository;
	private static final String STATUS_AVAILABLE = "AVAILABLE";

	public FlightService(FlightRepository flightRepository) {
		this.flightRepository = flightRepository;
	}

	@Transactional
	public Long addInventory(FlightInventoryRequest req) {
		validateInventoryRequest(req);

		boolean exists = flightRepository
				.existsByFlightNumberAndOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTime(req.getFlightNumber(),
						req.getOrigin(), req.getDestination(), req.getDeparture());

		if (exists) {
			String msg = String.format("Flight already exists: flightNumber=%s departure=%s origin=%s destination=%s",
					req.getFlightNumber(), req.getDeparture(), req.getOrigin(), req.getDestination());
			throw new ResponseStatusException(HttpStatus.CONFLICT, msg);
		}

		Flight flight = mapToEntity(req);

		int totalSeats = req.getTotalSeats();

		// ✈️ Standard narrow-body aircraft: 6 seats per row (A–F)
		char[] columns = {'A', 'B', 'C', 'D', 'E', 'F'};

		if (totalSeats % columns.length != 0) {
		    throw new ResponseStatusException(
		            HttpStatus.BAD_REQUEST,
		            "totalSeats must be a multiple of 6"
		    );
		}

		int totalRows = totalSeats / columns.length;

		List<FlightSeat> seats = new ArrayList<>(totalSeats);

		for (int row = 1; row <= totalRows; row++) {
		    for (char col : columns) {
		        FlightSeat s = new FlightSeat();
		        s.setSeatNumber(row + String.valueOf(col)); 
		        s.setStatus(STATUS_AVAILABLE);
		        s.setFlight(flight);
		        seats.add(s);
		    }
		}

		flight.setSeats(seats);

		Flight saved = flightRepository.save(flight);
		return saved.getId();
	}

	private void validateInventoryRequest(FlightInventoryRequest req) {
		if (req == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
		}
		if (isBlank(req.getAirline())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "airline is required");
		}
		if (isBlank(req.getFlightNumber())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "flightNumber is required");
		}
		if (isBlank(req.getOrigin()) || isBlank(req.getDestination())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "origin and destination are required");
		}
		if (req.getDeparture() == null || req.getArrival() == null) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departure and arrival are required");
		}
		if (!req.getArrival().isAfter(req.getDeparture())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "arrival must be after departure");
		}
		if (req.getDeparture().isBefore(LocalDateTime.now())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "departure cannot be in the past");
		}
		if (req.getOrigin().equalsIgnoreCase(req.getDestination())) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "origin and destination cannot be identical");
		}
		if (req.getTotalSeats() == null || req.getTotalSeats() <= 0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totalSeats must be > 0");
		}
		if (req.getPrice() == null || req.getPrice() <= 0.0) {
			throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "price must be > 0");
		}
	}

	private boolean isBlank(String s) {
		return s == null || s.trim().isEmpty();
	}

	private Flight mapToEntity(FlightInventoryRequest req) {
		Flight f = new Flight();
		f.setAirlineName(req.getAirline());
		f.setAirlineLogoUrl(req.getAirlineLogoUrl());
		f.setFlightNumber(req.getFlightNumber());
		f.setOrigin(req.getOrigin());
		f.setDestination(req.getDestination());
		f.setDepartureTime(req.getDeparture());
		f.setArrivalTime(req.getArrival());
		f.setTotalSeats(req.getTotalSeats());
		f.setPrice(req.getPrice());
		// default trip type to ONEWAY when not provided
		f.setTripType(isBlank(req.getTripType()) ? "ONEWAY" : req.getTripType());
		return f;
	}

	@Transactional(readOnly = true)
	public List<SearchResultDto> searchFlights(SearchRequest req) {

	    if (req == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Request body is required");
	    }
	    if (isBlank(req.getOrigin()) || isBlank(req.getDestination())) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "origin and destination are required");
	    }
	    if (req.getTravelDate() == null) {
	        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "travelDate is required");
	    }

	    LocalDate date = req.getTravelDate();
	    LocalDateTime start = date.atStartOfDay();
	    LocalDateTime end = date.atTime(LocalTime.MAX);

	    List<Flight> flights =
	        flightRepository.findByOriginIgnoreCaseAndDestinationIgnoreCaseAndDepartureTimeBetween(
	            req.getOrigin(), req.getDestination(), start, end
	        );
	    if (flights.isEmpty()) {
	        String msg = String.format(
	            "No flights found from %s to %s on %s",
	            req.getOrigin(),
	            req.getDestination(),
	            req.getTravelDate()
	        );
	        throw new ResponseStatusException(HttpStatus.NOT_FOUND, msg);
	    }

	    // filter by trip type if provided
	    if (!isBlank(req.getTripType())) {
	        flights = flights.stream()
	                .filter(f -> req.getTripType().equalsIgnoreCase(f.getTripType()))
	                .toList();

	        if (flights.isEmpty()) {
	            throw new ResponseStatusException(
	                HttpStatus.NOT_FOUND,
	                "No flights available for tripType: " + req.getTripType()
	            );
	        }
	    }

	    return flights.stream().map(f -> {
	        SearchResultDto r = new SearchResultDto();
	        r.setFlightId(f.getId());
	        r.setDepartureTime(f.getDepartureTime());
	        r.setArrivalTime(f.getArrivalTime());
	        r.setAirlineName(f.getAirlineName());
	        r.setAirlineLogoUrl(f.getAirlineLogoUrl());
	        r.setPrice(f.getPrice());
	        r.setTripType(f.getTripType());

	        int available = (int) Optional.ofNullable(f.getSeats())
	                .orElse(Collections.emptyList())
	                .stream()
	                .filter(s -> STATUS_AVAILABLE.equalsIgnoreCase(s.getStatus()))
	                .count();

	        r.setSeatsAvailable(available);
	        return r;
	    }).toList();
	}

	@Transactional(readOnly = true)
	public com.flightservice.dto.FlightDetailDto getDetailById(Long id) {
		if (id == null) {
			return null;
		}

		Optional<Flight> opt = flightRepository.findById(id);
		if (opt.isEmpty()) {
			return null;
		}

		Flight f = opt.get();

		com.flightservice.dto.FlightInfoDto info = new com.flightservice.dto.FlightInfoDto(f.getFlightNumber(),
				f.getAirlineName(), f.getAirlineLogoUrl(), f.getOrigin(), f.getDestination(), f.getDepartureTime(),
				f.getArrivalTime(), f.getPrice(), f.getTripType(), f.getTotalSeats());

		List<com.flightservice.dto.FlightDetailDto.SeatDto> seats = Optional.ofNullable(f.getSeats())
				.orElse(Collections.emptyList()).stream()
				.map(s -> new com.flightservice.dto.FlightDetailDto.SeatDto(s.getSeatNumber(), s.getStatus())).toList();

		return new com.flightservice.dto.FlightDetailDto(f.getId(), info, seats);
	}
	@Transactional
	public void markSeatsBooked(Long flightId, List<SeatBookingRequest> seats) {

	    Flight flight = flightRepository.findById(flightId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));

	    Map<String, String> seatToPassenger =
	            seats.stream().collect(Collectors.toMap(
	                    s -> s.getSeatNumber().trim().toUpperCase(),
	                    SeatBookingRequest::getPassengerName
	            ));

	    for (FlightSeat seat : flight.getSeats()) {
	        String seatNo = seat.getSeatNumber().toUpperCase();

	        if (seatToPassenger.containsKey(seatNo)) {

	            if (!"AVAILABLE".equalsIgnoreCase(seat.getStatus())) {
	                throw new ResponseStatusException(
	                        HttpStatus.CONFLICT,
	                        "Seat already booked: " + seatNo
	                );
	            }

	            seat.setStatus("BOOKED");
	            seat.setPassengerName(seatToPassenger.get(seatNo));
	        }
	    }

	    flightRepository.save(flight);
	}
	@Transactional(readOnly = true)
	public List<FlightSeatDto> getSeatMap(Long flightId) {

	    Flight flight = flightRepository.findById(flightId)
	            .orElseThrow(() ->
	                    new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));

	    return flight.getSeats().stream()
	            .map(seat -> new FlightSeatDto(
	                    seat.getSeatNumber(),
	                    seat.getStatus()
	            ))
	            .toList();
	}

	@Transactional
	public void markSeatsAvailable(Long flightId, List<String> seatNumbers) {

	    Flight flight = flightRepository.findById(flightId)
	        .orElseThrow(() ->
	            new ResponseStatusException(HttpStatus.NOT_FOUND, "Flight not found"));

	    Set<String> normalizedSeatNumbers =
	            seatNumbers.stream()
	                    .filter(Objects::nonNull)
	                    .map(s -> s.trim().toUpperCase())
	                    .collect(Collectors.toSet());

	    for (FlightSeat seat : flight.getSeats()) {

	        String seatNo = seat.getSeatNumber().trim().toUpperCase();

	        if (normalizedSeatNumbers.contains(seatNo)) {

	            seat.setStatus("AVAILABLE");
	            seat.setPassengerName(null);
	        }
	    }

	    flightRepository.save(flight);
	}
}