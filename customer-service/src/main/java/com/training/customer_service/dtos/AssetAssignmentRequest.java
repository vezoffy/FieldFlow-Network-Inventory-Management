package com.training.customer_service.dtos;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AssetAssignmentRequest {
    @NotBlank
    private String assetSerialNumber;
}
