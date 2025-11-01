package com.training.inventory_service.repositories;

import com.training.inventory_service.entities.Fdh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FdhRepository extends JpaRepository<Fdh, Long> {
    Optional<Fdh> findByCoreSwitchId(Long coreSwitchId);
    List<Fdh> findAllByCoreSwitchId(Long coreSwitchId); // New method
}
