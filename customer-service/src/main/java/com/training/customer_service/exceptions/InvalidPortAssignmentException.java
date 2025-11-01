package com.training.customer_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidPortAssignmentException extends RuntimeException {
    public InvalidPortAssignmentException(String message) {
        super(message);
    }
}
