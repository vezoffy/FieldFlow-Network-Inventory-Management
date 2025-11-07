package com.training.inventory_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetUpdateRequest {
    // Common fields
    private String location;
    private String model;

    // Infrastructure-specific fields
    private String name;
    private String region;
    private String neighborhood;
    private Integer portCapacity;
}
