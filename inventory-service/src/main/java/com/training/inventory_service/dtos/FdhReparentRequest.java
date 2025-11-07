package com.training.inventory_service.dtos;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class FdhReparentRequest {
    private Long newCoreSwitchId;
}
