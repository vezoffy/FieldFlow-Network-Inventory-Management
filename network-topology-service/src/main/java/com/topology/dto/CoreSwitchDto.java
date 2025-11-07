package com.topology.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CoreSwitchDto {
    private Long id;
    private String name;
    private String location;
    private Long headendId;
    private String serialNumber;
    private String model;
}
