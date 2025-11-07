package com.topology.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
public class FdhTopologyResponse {
    private Long fdhId;
    private String fdhName;
    private String region;
    private List<SplitterView> splitters;
}
