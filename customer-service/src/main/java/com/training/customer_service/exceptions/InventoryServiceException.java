package com.training.customer_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class InventoryServiceException extends RuntimeException {
    public InventoryServiceException(String message) {
        super(message);
    }
}
