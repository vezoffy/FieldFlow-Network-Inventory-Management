package com.training.inventory_service.dtos;

import lombok.Data;

import java.util.List;

@Data
public class HeadendTopologyDto {
    private Long id;
    private String name;
    private String location;
    private List<CoreSwitchTopologyDto> coreSwitches;
}
