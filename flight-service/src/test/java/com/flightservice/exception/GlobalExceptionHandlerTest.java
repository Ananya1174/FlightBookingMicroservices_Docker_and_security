package com.flightservice.exception;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

	private GlobalExceptionHandler handler;

	@BeforeEach
	void setUp() {
		handler = new GlobalExceptionHandler();
	}

	@Test
	void handleResponseStatus_createsCorrectBody_andStatus() {
		ResponseStatusException ex = new ResponseStatusException(HttpStatus.NOT_FOUND, "flight not found");

		var resp = handler.handleResponseStatus(ex);

		assertThat(resp).isNotNull().extracting(r -> r.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

		Map<String, Object> body = resp.getBody();
		assertThat(body).isNotNull().containsEntry("status", HttpStatus.NOT_FOUND.value())
				.containsEntry("error", HttpStatus.NOT_FOUND.getReasonPhrase())
				.containsEntry("message", "flight not found");

		assertThat(body.get("timestamp")).isNotNull();
	}

	@Test
	void handleValidation_returnsBadRequestWithErrors() {

		MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
		BindingResult bindingResult = mock(BindingResult.class);

		FieldError fe = new FieldError("obj", "field1", "must not be blank");
		when(bindingResult.getFieldErrors()).thenReturn(List.of(fe));
		when(ex.getBindingResult()).thenReturn(bindingResult);

		var resp = handler.handleValidation(ex);

		assertThat(resp).isNotNull().extracting(r -> r.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

		Map<String, Object> body = resp.getBody();
		assertThat(body).isNotNull().containsEntry("status", HttpStatus.BAD_REQUEST.value())
				.containsEntry("error", HttpStatus.BAD_REQUEST.getReasonPhrase())
				.containsEntry("message", "Validation failed").containsKey("errors");

		@SuppressWarnings("unchecked")
		List<String> errors = (List<String>) body.get("errors");
		assertThat(errors).hasSize(1).first().asString().contains("field1").contains("must not be blank");
	}

	@Test
	void handleAny_returnsInternalServerError() {
		RuntimeException rex = new RuntimeException("boom");

		var resp = handler.handleAny(rex);

		assertThat(resp).isNotNull().extracting(r -> r.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);

		Map<String, Object> body = resp.getBody();
		assertThat(body).isNotNull().containsEntry("status", HttpStatus.INTERNAL_SERVER_ERROR.value())
				.containsEntry("error", HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase())
				.containsEntry("message", "boom");

		assertThat(body.get("timestamp")).isNotNull();
	}
}