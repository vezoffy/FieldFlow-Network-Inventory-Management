package com.training.inventory_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitterDto {
    private Long id;
    private Long fdhId;
    private int portCapacity;
    private int usedPorts;
    private String serialNumber;
    private String neighborhood;
    private String model; // Added model field
}
