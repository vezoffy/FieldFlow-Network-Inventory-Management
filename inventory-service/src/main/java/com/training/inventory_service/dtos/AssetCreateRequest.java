package com.training.inventory_service.dtos;

import com.training.inventory_service.enums.AssetType;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetCreateRequest {

    @NotNull(message = "Asset type cannot be null")
    private AssetType assetType;

    // Common fields for most assets
    private String serialNumber;
    private String model;
    private String location;

    // Fields specific to Headend
    private String name; // Also used for FDH and CoreSwitch

    // Fields specific to CoreSwitch
    private Long headendId;

    // Fields specific to FDH
    private String region;
    private Long coreSwitchId;

    // Fields specific to Splitter
    private Long fdhId;
    private Integer portCapacity;
    private String neighborhood; // Added for Splitter
}
