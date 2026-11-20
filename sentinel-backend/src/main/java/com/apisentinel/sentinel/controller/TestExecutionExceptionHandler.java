package com.apisentinel.sentinel.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.Map;

@RestControllerAdvice
public class TestExecutionExceptionHandler {

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalState(
            IllegalStateException exception) {

        String message = exception.getMessage();

        if (message != null &&
                message.startsWith("Credentials are not configured for ")) {

            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    Map.of(
                            "status", "BLOCKED",
                            "reason", message,
                            "timestamp", Instant.now().toString()
                    )
            );
        }

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                Map.of(
                        "status", "ERROR",
                        "reason", "An internal execution error occurred",
                        "timestamp", Instant.now().toString()
                )
        );
    }
}
