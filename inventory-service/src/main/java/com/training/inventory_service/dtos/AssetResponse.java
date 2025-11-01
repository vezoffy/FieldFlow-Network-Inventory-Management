package com.training.inventory_service.dtos;

import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AssetResponse {
    private Long id;
    private String serialNumber;
    private AssetType assetType;
    private String model;
    private AssetStatus assetStatus;
    private String location;
    private Long assignedToCustomerId;
    private Instant createdAt;
}
