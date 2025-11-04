package com.training.customer_service.dtos;

import lombok.Getter;
import lombok.Setter;

// This DTO MUST mirror the structure of the AssetResponse from the inventory-service
@Getter
@Setter
public class AssetResponse {
    private Long id;
    private String serialNumber;
    private String assetType; // For cross-service communication
    private String model;
    private String assetStatus; // For cross-service communication
    private String location;
    private Long assignedToCustomerId;
}
