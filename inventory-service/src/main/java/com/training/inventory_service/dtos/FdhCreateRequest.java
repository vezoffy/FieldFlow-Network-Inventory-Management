package com.training.inventory_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FdhCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String region;
    @NotNull
    private Long headendId;
}
