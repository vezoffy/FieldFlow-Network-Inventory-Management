package com.training.inventory_service.dtos;

import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitterUpdateRequest {
    @Min(0)
    private int usedPorts;
}
