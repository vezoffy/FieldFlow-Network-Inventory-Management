package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class FdhTopologyResponse {
    private Long fdhId;
    private String fdhName;
    private String region;
    private List<SplitterView> splitters;
}
