package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TopologyTraceResponse {
    private Long customerId;
    private String customerName;
    private List<NetworkNode> path;
}
