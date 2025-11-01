package com.training.customer_service.entities;

import com.training.customer_service.enums.ConnectionType;
import com.training.customer_service.enums.CustomerStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "customers")
@Getter
@Setter
public class Customer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String name;

    @Column(nullable = false)
    private String address;

    private String neighborhood;

    private String plan;

    @Enumerated(EnumType.STRING)
    private ConnectionType connectionType;

    @Enumerated(EnumType.STRING)
    private CustomerStatus status;

    @Column(name = "splitter_serial_number")
    private String splitterSerialNumber;

    @Column(name = "splitter_id")
    private Long splitterId; // Re-added for redundant tracing

    private Integer assignedPort;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}
