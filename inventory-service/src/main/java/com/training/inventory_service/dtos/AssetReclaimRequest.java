package com.training.inventory_service.dtos;

import lombok.Data;

@Data
public class AssetReclaimRequest {
    private String status;
    private Long assignedToCustomer;
}
