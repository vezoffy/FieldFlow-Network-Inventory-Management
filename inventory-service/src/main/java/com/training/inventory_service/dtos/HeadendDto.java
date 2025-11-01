package com.training.inventory_service.dtos;

import lombok.Data;

@Data
public class HeadendDto {
    private Long id;
    private String name;
    private String location;
    private String serialNumber;
    private String model;
}
