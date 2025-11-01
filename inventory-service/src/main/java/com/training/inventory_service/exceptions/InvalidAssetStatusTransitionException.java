package com.training.inventory_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidAssetStatusTransitionException extends RuntimeException {
    public InvalidAssetStatusTransitionException(String message) {
        super(message);
    }
}
