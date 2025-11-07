package com.topology.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class SplitterDto {
    private Long id;
    private Long fdhId;
    private int portCapacity;
    private int usedPorts;
    private String serialNumber;
    private String neighborhood;
    private String model; // Added missing model field
    private List<CustomerAssignmentDto> customers;
}
