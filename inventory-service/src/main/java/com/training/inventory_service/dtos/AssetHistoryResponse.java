package com.training.inventory_service.dtos;

import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
public class AssetHistoryResponse {
    private Long id;
    private Long assetId;
    private String changeType;
    private String description;
    private Instant timestamp;
    private Long changedByUserId;
}
