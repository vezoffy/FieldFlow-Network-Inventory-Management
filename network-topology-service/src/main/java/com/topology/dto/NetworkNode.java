package com.topology.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor // Added for easier deserialization and construction
public class NetworkNode {
    private String type;
    private String identifier;
    private String detail;
    private String serialNumber; // Added
    private String model;        // Added
    private List<AssetDetailDto> assets;
}
