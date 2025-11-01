package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CustomerPathResponse {
    private Long customerId;
    private String customerName;
    private HierarchicalNetworkNode path;
}
