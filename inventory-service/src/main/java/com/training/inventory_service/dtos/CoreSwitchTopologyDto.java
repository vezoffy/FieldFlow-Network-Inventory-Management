package com.training.inventory_service.dtos;

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
