package com.topology.dto;

import lombok.Data;

import java.util.List;

@Data
public class FdhTopologyDto {
    private Long id;
    private String name;
    private String region;
    private Long coreSwitchId;
    private List<SplitterDto> splitters;
}
