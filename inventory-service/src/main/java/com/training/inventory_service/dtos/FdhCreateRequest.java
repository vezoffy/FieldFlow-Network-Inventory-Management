package com.training.inventory_service.dtos;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FdhCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String region;
    @NotNull
    private Long headendId;
}
