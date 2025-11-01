package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor; // Added NoArgsConstructor for flexibility

import java.util.List;

@Data
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
