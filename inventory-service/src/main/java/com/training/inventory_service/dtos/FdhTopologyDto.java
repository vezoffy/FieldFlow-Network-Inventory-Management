package com.training.inventory_service.dtos;

import lombok.Data;

import java.util.List;

@Data
public class FdhTopologyDto {
    private Long id;
    private String name;
    private String region;
    private Long coreSwitchId;
    private List<SplitterDto> splitters; // Reusing existing SplitterDto
}
