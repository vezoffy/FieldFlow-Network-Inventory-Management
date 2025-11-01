package com.training.inventory_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "core_switches")
@Getter
@Setter
public class CoreSwitch {

    @Id
    private Long id; // This will be the same as the Asset ID

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Asset asset;

    @Column(nullable = false, unique = true)
    private String name;

    private String location;

    private Long headendId;
}
