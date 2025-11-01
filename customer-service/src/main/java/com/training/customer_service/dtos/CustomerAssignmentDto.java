package com.training.customer_service.dtos;

import com.training.customer_service.dtos.feign.AssetDetailDto;
import lombok.Data;

import java.util.List;

@Data
public class CustomerAssignmentDto {
    private Long customerId;
    private String name;
    private Long splitterId;
    private int assignedPort;
    private String status;
    private List<AssetDetailDto> assignedAssets;
}
