package com.deploymentservice.exceptions;

public class DeploymentTaskNotFoundException extends RuntimeException {
    public DeploymentTaskNotFoundException(String message) {
        super(message);
    }
}
