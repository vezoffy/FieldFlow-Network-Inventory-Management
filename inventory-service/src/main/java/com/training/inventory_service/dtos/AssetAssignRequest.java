package com.training.inventory_service.dtos;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetAssignRequest {
    @NotNull
    private Long customerId;
}
