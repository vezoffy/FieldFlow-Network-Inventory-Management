package com.topology.dto;

import lombok.Data;

import java.util.List;

@Data
public class CustomerAssignmentDto {
    private Long customerId;
    private String name;
    private Long splitterId;
    private int assignedPort;
    private String status;
    private List<AssetDetailDto> assignedAssets;
}
