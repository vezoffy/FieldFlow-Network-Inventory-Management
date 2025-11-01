package com.training.inventory_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class HeadendCreateRequest {
    @NotBlank
    private String name;
    @NotBlank
    private String location;
}
