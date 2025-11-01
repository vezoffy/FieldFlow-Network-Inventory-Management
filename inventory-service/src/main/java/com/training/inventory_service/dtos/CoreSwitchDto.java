package com.training.inventory_service.dtos;

import lombok.Data;

@Data
public class CoreSwitchDto {
    private Long id;
    private String name;
    private String location;
    private Long headendId;
    private String serialNumber;
    private String model;
}
