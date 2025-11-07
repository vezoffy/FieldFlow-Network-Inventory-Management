package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class SplitterView {
    private Long splitterId;
    private int capacity;
    private int used;
    private List<CustomerAssignmentDto> connectedCustomers;
}
