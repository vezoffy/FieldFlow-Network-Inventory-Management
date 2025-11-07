package com.training.inventory_service.exceptions;

public class InvalidAssetStatusTransitionException extends RuntimeException {
    public InvalidAssetStatusTransitionException(String message) {
        super(message);
    }
}
