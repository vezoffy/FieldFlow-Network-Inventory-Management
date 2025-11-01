package com.training.inventory_service.dtos;

import jakarta.validation.constraints.Min;
import lombok.Data;

@Data
public class SplitterUpdateRequest {
    @Min(0)
    private int usedPorts;
}
