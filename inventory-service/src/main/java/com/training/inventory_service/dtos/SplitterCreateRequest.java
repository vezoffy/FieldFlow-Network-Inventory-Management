package com.training.inventory_service.dtos;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitterCreateRequest {
    @NotNull
    private Long fdhId;
    @Min(1)
    private int portCapacity;
}
