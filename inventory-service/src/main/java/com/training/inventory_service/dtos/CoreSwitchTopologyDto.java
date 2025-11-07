package com.training.inventory_service.dtos;


import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CoreSwitchTopologyDto {
    private Long id;
    private String name;
    private String location;
    private Long headendId;
    private List<FdhTopologyDto> fdhs;
}
