package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class CustomerPathResponse {
    private Long customerId;
    private String customerName;
    private HierarchicalNetworkNode path;
}
