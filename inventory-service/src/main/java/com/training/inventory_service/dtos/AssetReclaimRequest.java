package com.training.inventory_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetReclaimRequest {
    private String status;
    private Long assignedToCustomer;
}
