package com.training.inventory_service.exceptions;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@Setter
public class ErrorDetails {
    private LocalDateTime timestamp;
    private String message;
    private String details;
    private Map<String, String> validationErrors;

    public ErrorDetails(LocalDateTime timestamp, String message, String details) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
    }

    public ErrorDetails(LocalDateTime timestamp, String message, String details, Map<String, String> validationErrors) {
        this.timestamp = timestamp;
        this.message = message;
        this.details = details;
        this.validationErrors = validationErrors;
    }
}
