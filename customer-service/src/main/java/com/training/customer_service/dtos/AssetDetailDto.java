package com.training.customer_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetDetailDto {
    private String assetType;
    private String serialNumber;
    private String model;
}
