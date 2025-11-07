package com.training.customer_service.exceptions;

public class InvalidPortAssignmentException extends RuntimeException {
    public InvalidPortAssignmentException(String message) {
        super(message);
    }
}
