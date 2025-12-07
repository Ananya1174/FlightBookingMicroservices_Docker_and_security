package com.flightservice.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;

import java.util.Map;

import static org.assertj.core.api.Assertions.*;

class GlobalExceptionHandlerTest {

    GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void handleResponseStatusException_returnsMapWithReason() {
        ResponseStatusException ex = new ResponseStatusException(HttpStatus.CONFLICT, "duplicate");
        var resp = handler.handleResponseStatus(ex);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
        assertThat(resp.getBody().get("status")).isEqualTo(HttpStatus.CONFLICT.value());
        assertThat(resp.getBody().get("message")).isEqualTo("duplicate");
    }

    @Test
    void handleValidation_returnsErrorsList() {
        // create a MethodArgumentNotValidException with one field error
        BeanPropertyBindingResult binding = new BeanPropertyBindingResult(new Object(), "target");
        binding.addError(new FieldError("target", "origin", "must not be blank"));
        MethodArgumentNotValidException mex = new MethodArgumentNotValidException(null, binding);
        var resp = handler.handleValidation(mex);
        assertThat(resp.getBody()).isInstanceOf(Map.class);
        assertThat(((Map<?,?>)resp.getBody()).get("status")).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(((Map<?,?>)resp.getBody()).get("errors")).toString().contains("origin");
    }

    @Test
    void handleAny_returns500() {
        var resp = handler.handleAny(new RuntimeException("boom"));
        assertThat(resp.getBody().get("status")).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR.value());
    }
}