package com.flightservice.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {

    // reuse these keys to satisfy the "define a constant" Sonar suggestions
    private static final String KEY_TIMESTAMP = "timestamp";
    private static final String KEY_STATUS = "status";
    private static final String KEY_ERROR = "error";
    private static final String KEY_MESSAGE = "message";
    private static final String KEY_ERRORS = "errors";

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String,Object>> handleResponseStatus(ResponseStatusException ex) {
        Map<String,Object> body = new HashMap<>();
        body.put(KEY_TIMESTAMP, Instant.now());
        body.put(KEY_STATUS, ex.getStatusCode().value());
        HttpStatus httpStatus = HttpStatus.resolve(ex.getStatusCode().value());
        body.put(KEY_ERROR, httpStatus != null ? httpStatus.getReasonPhrase() : "Error");
        // ex.getReason() is the message passed when throwing ResponseStatusException
        body.put(KEY_MESSAGE, ex.getReason());
        return new ResponseEntity<>(body, ex.getStatusCode());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String,Object>> handleValidation(MethodArgumentNotValidException ex) {
        List<String> errors = ex.getBindingResult()
                               .getFieldErrors()
                               .stream()
                               .map(f -> f.getField() + ": " + f.getDefaultMessage())
                               .toList();

        Map<String,Object> body = new HashMap<>();
        body.put(KEY_TIMESTAMP, Instant.now());
        body.put(KEY_STATUS, HttpStatus.BAD_REQUEST.value());
        body.put(KEY_ERROR, HttpStatus.BAD_REQUEST.getReasonPhrase());
        body.put(KEY_MESSAGE, "Validation failed");
        body.put(KEY_ERRORS, errors);

        return new ResponseEntity<>(body, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String,Object>> handleAny(Exception ex) {
        Map<String,Object> body = new HashMap<>();
        body.put(KEY_TIMESTAMP, Instant.now());
        body.put(KEY_STATUS, HttpStatus.INTERNAL_SERVER_ERROR.value());
        body.put(KEY_ERROR, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
        body.put(KEY_MESSAGE, ex.getMessage());
        return new ResponseEntity<>(body, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}