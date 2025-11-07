package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class TopologyTraceResponse {
    private Long customerId;
    private String customerName;
    private List<NetworkNode> path;
}
