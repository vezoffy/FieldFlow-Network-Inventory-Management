package com.topology.exceptions;

public class TopologyServiceException extends RuntimeException {
    public TopologyServiceException(String message) {
        super(message);
    }

    public TopologyServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
