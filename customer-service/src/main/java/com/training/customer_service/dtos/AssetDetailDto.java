package com.training.customer_service.dtos;

import lombok.Data;

@Data
public class AssetDetailDto {
    private String assetType;
    private String serialNumber;
    private String model;
}
