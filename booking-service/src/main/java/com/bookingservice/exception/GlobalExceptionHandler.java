package com.bookingservice.exception;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;

import jakarta.servlet.http.HttpServletRequest;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	@ExceptionHandler(ResponseStatusException.class)
	public ResponseEntity<Map<String, Object>> handleResponseStatusException(ResponseStatusException ex,
			HttpServletRequest req) {
		Map<String, Object> body = standardBody(ex.getStatusCode(),
				Optional.ofNullable(ex.getReason()).orElse(ex.getMessage()), req.getRequestURI());
		return new ResponseEntity<>(body, ex.getStatusCode());
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<String> errors = ex.getBindingResult().getFieldErrors().stream()
				.map(f -> f.getField() + ": " + defaultMessage(f)).collect(Collectors.toList());

		String path = extractPath(request);
		Map<String, Object> body = standardBody(HttpStatusCode.valueOf(400), String.join("; ", errors), path);
		return ResponseEntity.badRequest().body(body);
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(
			org.springframework.http.converter.HttpMessageNotReadableException ex, HttpHeaders headers,
			HttpStatusCode status, WebRequest request) {
		String msg = "Malformed request body: "
				+ Optional.ofNullable(ex.getMostSpecificCause()).map(Throwable::getMessage).orElse(ex.getMessage());
		String path = extractPath(request);
		Map<String, Object> body = standardBody(HttpStatusCode.valueOf(400), msg, path);
		return ResponseEntity.badRequest().body(body);
	}

	@ExceptionHandler(DataIntegrityViolationException.class)
	public ResponseEntity<Map<String, Object>> handleDataIntegrity(DataIntegrityViolationException ex,
			HttpServletRequest req) {
		String detail = Optional.ofNullable(ex.getMostSpecificCause()).map(Throwable::getMessage)
				.orElse(ex.getMessage());
		Map<String, Object> body = standardBody(HttpStatusCode.valueOf(409), "Database error: " + detail,
				req.getRequestURI());
		return ResponseEntity.status(409).body(body);
	}

	@ExceptionHandler({ ObjectOptimisticLockingFailureException.class })
	public ResponseEntity<Map<String, Object>> handleOptimisticLock(Exception ex, HttpServletRequest req) {
		Map<String, Object> body = standardBody(HttpStatusCode.valueOf(409), "Concurrent update. Please retry.",
				req.getRequestURI());
		return ResponseEntity.status(409).body(body);
	}

	@ExceptionHandler(Exception.class)
	public ResponseEntity<Map<String, Object>> handleAll(Exception ex, HttpServletRequest req) {

		ex.printStackTrace();
		Map<String, Object> body = standardBody(HttpStatusCode.valueOf(500), "Internal server error",
				req.getRequestURI());
		return ResponseEntity.status(500).body(body);
	}

	private Map<String, Object> standardBody(HttpStatusCode statusCode, String message, String path) {
		int statusValue = statusCode.value();
		String reason = Optional.ofNullable(HttpStatus.resolve(statusValue)).map(HttpStatus::getReasonPhrase)
				.orElse(statusCode.toString());
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("timestamp", Instant.now().toString());
		body.put("status", statusValue);
		body.put("error", reason);
		body.put("message", message);
		body.put("path", path);
		return body;
	}

	private String defaultMessage(FieldError f) {
		return f.getDefaultMessage() != null ? f.getDefaultMessage() : "invalid";
	}

	private String extractPath(WebRequest request) {
		if (request instanceof ServletWebRequest servlet) {
			return servlet.getRequest().getRequestURI();
		}
		return "N/A";
	}
}