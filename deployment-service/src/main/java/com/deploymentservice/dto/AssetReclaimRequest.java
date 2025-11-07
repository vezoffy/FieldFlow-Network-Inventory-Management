package com.deploymentservice.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetReclaimRequest {
    private String status;
    private Long assignedToCustomer;
}
