package com.topology.dto;

import lombok.Data;

import java.util.List;

@Data
public class CoreSwitchTopologyDto {
    private Long id;
    private String name;
    private String location;
    private Long headendId;
    private List<FdhTopologyDto> fdhs;
}
