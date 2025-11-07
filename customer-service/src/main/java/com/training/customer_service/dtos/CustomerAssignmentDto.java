package com.training.customer_service.dtos;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class CustomerAssignmentDto {
    private Long customerId;
    private String name;
    private Long splitterId;
    private int assignedPort;
    private String status;
    private List<AssetDetailDto> assignedAssets;
}
