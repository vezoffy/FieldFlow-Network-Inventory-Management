package com.training.inventory_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

//@ResponseStatus(HttpStatus.CONFLICT)
public class AssetAlreadyExistsException extends RuntimeException {
    public AssetAlreadyExistsException(String message) {
        super(message);
    }
}
