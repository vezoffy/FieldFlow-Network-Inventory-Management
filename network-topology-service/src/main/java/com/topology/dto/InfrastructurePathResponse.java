package com.topology.dto;

import com.topology.enums.AssetType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructurePathResponse {
    private String startDeviceSerialNumber;
    private AssetType startDeviceType;
    private HierarchicalNetworkNode path; // Changed to a single hierarchical node
}
