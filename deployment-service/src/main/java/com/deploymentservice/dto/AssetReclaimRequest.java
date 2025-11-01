package com.deploymentservice.dto;

import lombok.Data;

@Data
public class AssetReclaimRequest {
    private String status;
    private Long assignedToCustomer;
}
