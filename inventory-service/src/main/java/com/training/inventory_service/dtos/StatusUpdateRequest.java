package com.training.inventory_service.dtos;

import com.training.inventory_service.enums.AssetStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class StatusUpdateRequest {
    @NotNull(message = "New status cannot be null")
    private AssetStatus newStatus;
}
