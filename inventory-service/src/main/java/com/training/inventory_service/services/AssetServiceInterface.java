package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;

import java.util.List;

public interface AssetServiceInterface {
    AssetResponse replaceFaultyAsset(AssetReplacementRequest request, Long userId);
    AssetResponse unassignCustomerAssetsBySerialNumber(String serialNumber);
    void deleteAsset(Long assetId);
    AssetResponse createAsset(AssetCreateRequest request);
    AssetResponse getAssetBySerial(String serialNumber);
    AssetAssignmentDetailsDto getAssetAssignmentDetails(String serialNumber);
    List<AssetResponse> getAssetsByCustomerId(Long customerId);
    List<AssetResponse> filterAssets(AssetType type, AssetStatus status, String location);
    AssetResponse assignAssetToCustomer(String serialNumber, Long customerId, Long userId);
    void unassignAssetsFromCustomer(Long customerId, String newStatus, Long userId);
    AssetResponse updateAssetStatus(Long id, AssetStatus newStatus, Long userId);
    List<AssetHistoryResponse> getAssetHistory(Long id);
    List<AssetResponse> getFaultyAssignedAssets();
}
