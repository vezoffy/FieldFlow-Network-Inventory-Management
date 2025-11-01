package com.training.inventory_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "asset_history")
@Getter
@Setter
public class AssetHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long assetId;

    @Column(nullable = false)
    private String changeType;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Instant timestamp = Instant.now();

    private Long changedByUserId;
}
