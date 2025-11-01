package com.training.inventory_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "headends")
@Getter
@Setter
public class Headend {

    @Id
    private Long id; // This will be the same as the Asset ID

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId // Maps the 'id' field to the Asset's ID
    @JoinColumn(name = "id")
    private Asset asset;

    @Column(nullable = false, unique = true)
    private String name;

    private String location;
}
