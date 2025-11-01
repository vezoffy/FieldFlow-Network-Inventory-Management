package com.topology.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class TopologyServiceException extends RuntimeException {
    public TopologyServiceException(String message) {
        super(message);
    }

    public TopologyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
