package com.training.inventory_service.entities;

import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(name = "assets")
@Getter
@Setter
public class Asset {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String serialNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetType assetType;

    private String model;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AssetStatus assetStatus;

    private String location;

    private Long assignedToCustomerId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
