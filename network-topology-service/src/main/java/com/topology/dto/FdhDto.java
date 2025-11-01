package com.topology.dto;

import lombok.Data;

@Data
public class FdhDto {
    private Long id;
    private String name;
    private String region;
    private Long coreSwitchId;
    private String serialNumber;
    private String model;
}
