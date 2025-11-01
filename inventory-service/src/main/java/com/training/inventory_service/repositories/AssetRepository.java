package com.training.inventory_service.repositories;

import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long> {
    Optional<Asset> findBySerialNumber(String serialNumber);
    boolean existsBySerialNumber(String serialNumber);
    List<Asset> findByAssetTypeAndAssetStatus(AssetType type, AssetStatus status);
    List<Asset> findByAssignedToCustomerId(Long customerId); // New method
}
