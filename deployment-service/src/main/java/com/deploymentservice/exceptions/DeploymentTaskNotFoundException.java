package com.deploymentservice.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class DeploymentTaskNotFoundException extends RuntimeException {
    public DeploymentTaskNotFoundException(String message) {
        super(message);
    }
}
