package com.topology.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class HierarchicalNetworkNode {
    private String type;
    private String identifier;
    private String detail;
    private String serialNumber;
    private String model;
    private List<AssetDetailDto> assets;
    private HierarchicalNetworkNode child;

    public HierarchicalNetworkNode(String type, String identifier, String detail, String serialNumber, String model) {
        this.type = type;
        this.identifier = identifier;
        this.detail = detail;
        this.serialNumber = serialNumber;
        this.model = model;
    }
}
