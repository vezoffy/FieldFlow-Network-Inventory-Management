package com.topology.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class HeadendTopologyDto {
    private Long id;
    private String name;
    private String location;
    private List<CoreSwitchTopologyDto> coreSwitches;
}
