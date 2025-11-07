package com.training.inventory_service.repositories;

import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;

@Repository
public interface AssetRepository extends JpaRepository<Asset, Long>, JpaSpecificationExecutor<Asset>{
    boolean existsBySerialNumber(String serialNumber);
    Optional<Asset> findBySerialNumber(String serialNumber);
    List<Asset> findByAssignedToCustomerId(Long customerId);

    // New method to find faulty, assigned ONTs and Routers
    @Query("SELECT a FROM Asset a WHERE a.assetStatus = :status AND a.assignedToCustomerId IS NOT NULL AND a.assetType IN :types")
    List<Asset> findByStatusAndAssignedAndType(AssetStatus status, List<AssetType> types);
}
