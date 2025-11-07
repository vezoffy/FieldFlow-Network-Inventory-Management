package com.training.customer_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetAssignmentRequest {
    @NotBlank
    private String assetSerialNumber;
}
