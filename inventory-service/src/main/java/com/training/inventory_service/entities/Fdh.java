package com.training.inventory_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "fdhs")
@Getter
@Setter
public class Fdh {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Asset asset;

    @Column(nullable = false, unique = true)
    private String name;

    private String region;

    @Column(name = "core_switch_id")
    private Long coreSwitchId; // Changed from headendId
}
