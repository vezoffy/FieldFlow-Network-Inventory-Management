package com.topology.dto;

import com.topology.enums.AssetType;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class InfrastructurePathResponse {
    private String startDeviceSerialNumber;
    private AssetType startDeviceType;
    private HierarchicalNetworkNode path; // Changed to a single hierarchical node
}
