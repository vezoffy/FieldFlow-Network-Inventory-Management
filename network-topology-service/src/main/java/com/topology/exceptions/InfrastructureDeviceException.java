package com.topology.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InfrastructureDeviceException extends RuntimeException {
    public InfrastructureDeviceException(String message) {
        super(message);
    }
}
