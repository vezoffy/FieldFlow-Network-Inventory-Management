package com.training.customer_service.dtos;

import com.training.customer_service.dtos.feign.AssetResponse;
import com.training.customer_service.enums.ConnectionType;
import com.training.customer_service.enums.CustomerStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
public class CustomerResponse {
    private Long id;
    private String name;
    private String address;
    private String neighborhood;
    private String plan;
    private ConnectionType connectionType;
    private CustomerStatus status;
    private String splitterSerialNumber;
    private Long splitterId; // Re-added
    private Integer assignedPort;
    private Instant createdAt;
    private List<AssetResponse> assignedAssets;
}
