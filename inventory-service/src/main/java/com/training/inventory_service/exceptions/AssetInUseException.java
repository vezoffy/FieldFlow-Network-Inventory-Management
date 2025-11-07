package com.training.inventory_service.exceptions;

public class AssetInUseException extends RuntimeException {
    public AssetInUseException(String message) {
        super(message);
    }
}
