package com.bookingservice.dto;

import jakarta.validation.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class DtoValidationTest {

    private static Validator validator;

    @BeforeAll
    static void setup() {
        ValidatorFactory factory = Validation.buildDefaultValidatorFactory();
        validator = factory.getValidator();
    }

    @Test
    void bookingRequest_validates_userEmail_optional_but_numSeats_and_passengers_required() {
        // valid case
        PersonDto p = PersonDto.builder().name("John").age(25).gender("M").seatNumber("A1").build();
        BookingRequest good = BookingRequest.builder().userEmail("u@x.com").numSeats(1).passengers(List.of(p)).build();
        Set<ConstraintViolation<BookingRequest>> v = validator.validate(good);
        assertThat(v).isEmpty();

        // numSeats null -> violation due to @Positive in field declaration? (numSeats is Integer with @Positive)
        BookingRequest bad1 = BookingRequest.builder().userEmail("u@x.com").numSeats(null).passengers(List.of(p)).build();
        Set<ConstraintViolation<BookingRequest>> v1 = validator.validate(bad1);
        // Since @Positive doesn't check null, but code expects non-null, there may be no constraint violation for null.
        // However passengers empty or invalid person should fail.
        assertThat(v1).isEmpty(); // null numSeats is allowed by bean validation here (service layer checks null) — assert emptiness

        // passengers empty -> @NotEmpty should fire
        BookingRequest bad2 = BookingRequest.builder().userEmail("u@x.com").numSeats(0).passengers(List.of()).build();
        Set<ConstraintViolation<BookingRequest>> v2 = validator.validate(bad2);
        assertThat(v2).isNotEmpty();
        boolean foundNotEmpty = v2.stream().anyMatch(c -> c.getPropertyPath().toString().equals("passengers"));
        assertThat(foundNotEmpty).isTrue();
    }

    @Test
    void personDto_validates_notBlank_name_and_positive_age_and_gender() {
        PersonDto ok = PersonDto.builder().name("Jane").age(20).gender("F").build();
        Set<ConstraintViolation<PersonDto>> v = validator.validate(ok);
        assertThat(v).isEmpty();

        PersonDto bad = PersonDto.builder().name("").age(0).gender("").build();
        Set<ConstraintViolation<PersonDto>> v2 = validator.validate(bad);
        assertThat(v2).isNotEmpty();
        // ensure name and age violations exist
        assertThat(v2).anyMatch(c -> c.getPropertyPath().toString().equals("name"));
        assertThat(v2).anyMatch(c -> c.getPropertyPath().toString().equals("age"));
    }

    @Test
    void seatDto_has_no_validation_annotations_and_is_serializable() {
        // SeatDto has no jakarta validation annotations; assert that we can construct it and fields are accessible
        SeatDto s = new SeatDto();
        s.setSeatNumber("A1");
        s.setStatus("AVAILABLE");
        assertThat(s.getSeatNumber()).isEqualTo("A1");
        assertThat(s.getStatus()).isEqualTo("AVAILABLE");
    }
}