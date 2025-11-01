package com.training.customer_service.repositories;

import com.training.customer_service.entities.FiberDropLine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface FiberDropLineRepository extends JpaRepository<FiberDropLine, Long> {
    Optional<FiberDropLine> findByCustomerId(Long customerId);
}
