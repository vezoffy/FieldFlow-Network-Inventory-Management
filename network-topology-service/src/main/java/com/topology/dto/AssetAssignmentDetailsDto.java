package com.topology.dto;

import com.topology.enums.AssetType;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetAssignmentDetailsDto {
    private Long assetId; // Added to hold the ID of the current asset
    private Long customerId; // Null if not assigned to a customer
    private String assetSerialNumber;
    private AssetType assetType;
    private Long nextDeviceId; // ID of the next device in the hierarchy (e.g., CoreSwitch for Headend)
    private String nextDeviceSerialNumber; // Added to hold the serial number of the next device
}
