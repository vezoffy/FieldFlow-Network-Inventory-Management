package com.topology.dto;

import com.topology.enums.AssetType;
import lombok.Data;

import java.time.Instant;

@Data
public class AssetResponse {
    private Long id;
    private String serialNumber;
    private AssetType assetType;
    private String model;
    private String assetStatus;
    private String location;
    private Long assignedToCustomerId;
    private Instant createdAt;
}
