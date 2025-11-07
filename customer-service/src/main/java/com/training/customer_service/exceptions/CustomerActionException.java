package com.training.customer_service.exceptions;

public class CustomerActionException extends RuntimeException {
    public CustomerActionException(String message) {
        super(message);
    }
}
