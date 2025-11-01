package com.training.inventory_service.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "splitters")
@Getter
@Setter
public class Splitter {

    @Id
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "id")
    private Asset asset;

    private Long fdhId;

    private int portCapacity;

    private int usedPorts;

    @Column(name = "neighborhood") // Explicitly defining the column
    private String neighborhood;
}
