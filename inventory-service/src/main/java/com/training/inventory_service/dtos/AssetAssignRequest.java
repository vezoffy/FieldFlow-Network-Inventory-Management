package com.training.inventory_service.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class AssetAssignRequest {
    @NotNull
    private Long customerId;
}
