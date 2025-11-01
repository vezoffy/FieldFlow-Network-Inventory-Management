package com.training.inventory_service.repositories;

import com.training.inventory_service.entities.AssetHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AssetHistoryRepository extends JpaRepository<AssetHistory, Long> {
    List<AssetHistory> findByAssetIdOrderByTimestampDesc(Long assetId);
}
