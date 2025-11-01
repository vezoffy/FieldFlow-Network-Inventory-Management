package com.training.inventory_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetUpdateRequest {
    private String model;
    private String location;
}
