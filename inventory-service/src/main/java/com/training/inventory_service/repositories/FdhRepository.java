package com.training.inventory_service.repositories;

import com.training.inventory_service.entities.Fdh;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface FdhRepository extends JpaRepository<Fdh, Long>, JpaSpecificationExecutor<Fdh> {
    List<Fdh> findAllByCoreSwitchId(Long coreSwitchId);
    boolean existsByCoreSwitchId(Long coreSwitchId);
    Optional<Fdh> findByAssetId(Long assetId);
    void deleteByAssetId(Long assetId);
    List<Fdh> findAllByCoreSwitchIdIn(List<Long> coreSwitchIds);
}
