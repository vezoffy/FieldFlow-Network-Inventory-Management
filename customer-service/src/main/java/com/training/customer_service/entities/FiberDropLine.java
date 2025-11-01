package com.training.customer_service.entities;

import com.training.customer_service.enums.FiberStatus;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import java.math.BigDecimal;

@Entity
@Table(name = "fiber_drop_line")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class FiberDropLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "customer_id", unique = true, nullable = false)
    private Long customerId;

    @Column(name = "from_splitter_id")
    private Long fromSplitterId;

    @Column(name = "length_meters", precision = 6, scale = 2)
    private BigDecimal lengthMeters;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private FiberStatus status;
}
