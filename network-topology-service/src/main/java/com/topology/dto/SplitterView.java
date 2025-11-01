package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class SplitterView {
    private Long splitterId;
    private int capacity;
    private int used;
    private List<CustomerAssignmentDto> connectedCustomers;
}
