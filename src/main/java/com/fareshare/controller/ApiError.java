package com.fareshare.controller;

import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

public final class ApiError extends RuntimeException {
    final HttpStatus status;
    public ApiError(HttpStatus status, String message) { super(message); this.status = status; }
    public static ApiError bad(String message) { return new ApiError(HttpStatus.BAD_REQUEST, message); }
    public static ApiError forbidden(String message) { return new ApiError(HttpStatus.FORBIDDEN, message); }
    public static ApiError missing(String message) { return new ApiError(HttpStatus.NOT_FOUND, message); }
    public static ApiError conflict(String message) { return new ApiError(HttpStatus.CONFLICT, message); }
}

@RestControllerAdvice
class ApiErrors {
    @ExceptionHandler(ApiError.class)
    ResponseEntity<Map<String, Object>> known(ApiError error) {
        return ResponseEntity.status(error.status).body(Map.of(
                "status", error.status.value(), "error", error.getMessage(), "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<Map<String, Object>> invalid(MethodArgumentNotValidException error) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "error", "Invalid request body", "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(ObjectOptimisticLockingFailureException.class)
    ResponseEntity<Map<String, Object>> concurrent(ObjectOptimisticLockingFailureException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409, "error", "Record changed; refresh and retry", "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler({HttpMessageNotReadableException.class, MissingRequestHeaderException.class})
    ResponseEntity<Map<String, Object>> malformed(Exception error) {
        return ResponseEntity.badRequest().body(Map.of(
                "status", 400, "error", "Malformed request or missing required header",
                "timestamp", Instant.now().toString()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    ResponseEntity<Map<String, Object>> duplicate(DataIntegrityViolationException error) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of(
                "status", 409, "error", "Conflicting or duplicate record",
                "timestamp", Instant.now().toString()));
    }
}
