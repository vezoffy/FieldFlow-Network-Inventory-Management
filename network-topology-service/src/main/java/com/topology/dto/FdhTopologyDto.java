package com.topology.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class FdhTopologyDto {
    private Long id;
    private String name;
    private String region;
    private Long coreSwitchId;
    private List<SplitterDto> splitters;
}
