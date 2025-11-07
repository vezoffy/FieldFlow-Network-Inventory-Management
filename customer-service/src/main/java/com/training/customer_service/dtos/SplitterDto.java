package com.training.customer_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SplitterDto {
    private Long id;
    private Long fdhId;
    private int portCapacity;
    private int usedPorts;
    private String serialNumber; // Assuming inventory-service SplitterDto has this
}
