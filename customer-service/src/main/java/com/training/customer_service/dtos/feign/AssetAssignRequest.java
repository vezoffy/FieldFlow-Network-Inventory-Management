package com.training.customer_service.dtos.feign;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AssetAssignRequest {
    private Long customerId;
}
