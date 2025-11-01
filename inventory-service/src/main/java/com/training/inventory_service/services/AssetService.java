package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.entities.AssetHistory;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.exceptions.AssetAlreadyExistsException;
import com.training.inventory_service.exceptions.AssetNotFoundException;
import com.training.inventory_service.repositories.AssetRepository;
import com.training.inventory_service.repositories.AssetHistoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class AssetService {

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Transactional
    public AssetResponse createAsset(AssetCreateRequest request) {
        if (request.getSerialNumber() != null && assetRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new AssetAlreadyExistsException("Asset with serial number " + request.getSerialNumber() + " already exists.");
        }

        Asset asset = new Asset();
        asset.setAssetType(request.getAssetType());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setModel(request.getModel());
        asset.setLocation(request.getLocation());
        asset.setAssetStatus(AssetStatus.AVAILABLE);
        asset.setCreatedAt(Instant.now());

        Asset savedAsset = assetRepository.save(asset);

        logAssetHistory(savedAsset.getId(), "ASSET_CREATED", "New asset created.", null);

        return mapToAssetResponse(savedAsset);
    }

    public AssetResponse getAssetBySerial(String serialNumber) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException("Asset with serial number " + serialNumber + " not found."));
        return mapToAssetResponse(asset);
    }

    public AssetAssignmentDetailsDto getAssetAssignmentDetails(String serialNumber) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException("Asset with serial number " + serialNumber + " not found."));

        AssetAssignmentDetailsDto dto = new AssetAssignmentDetailsDto();
        dto.setAssetSerialNumber(asset.getSerialNumber());
        dto.setAssetType(asset.getAssetType());
        dto.setAssetId(asset.getId());

        if (asset.getAssignedToCustomerId() != null) {
            dto.setCustomerId(asset.getAssignedToCustomerId());
        }

        return dto;
    }

    public List<AssetResponse> getAssetsByCustomerId(Long customerId) {
        List<Asset> assets = assetRepository.findByAssignedToCustomerId(customerId);
        return assets.stream().map(this::mapToAssetResponse).collect(Collectors.toList());
    }

    public List<AssetResponse> filterAssets(AssetType type, AssetStatus status, String location) {
        List<Asset> assets = assetRepository.findAll();
        if (type != null) {
            assets = assets.stream().filter(a -> a.getAssetType() == type).collect(Collectors.toList());
        }
        if (status != null) {
            assets = assets.stream().filter(a -> a.getAssetStatus() == status).collect(Collectors.toList());
        }
        if (location != null && !location.isBlank()) {
            assets = assets.stream().filter(a -> location.equalsIgnoreCase(a.getLocation())).collect(Collectors.toList());
        }
        return assets.stream().map(this::mapToAssetResponse).collect(Collectors.toList());
    }

    @Transactional
    public AssetResponse assignAssetToCustomer(String serialNumber, Long customerId, Long userId) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException("Asset with serial number " + serialNumber + " not found."));

        asset.setAssignedToCustomerId(customerId);
        asset.setAssetStatus(AssetStatus.ASSIGNED);
        Asset updatedAsset = assetRepository.save(asset);

        logAssetHistory(updatedAsset.getId(), "ASSET_ASSIGNED", "Assigned to customer ID: " + customerId, userId);

        return mapToAssetResponse(updatedAsset);
    }

    @Transactional
    public void unassignAssetsFromCustomer(Long customerId, String newStatus, Long userId) {
        List<Asset> assets = assetRepository.findByAssignedToCustomerId(customerId);
        AssetStatus status = AssetStatus.valueOf(newStatus.toUpperCase());

        for (Asset asset : assets) {
            asset.setAssignedToCustomerId(null);
            asset.setAssetStatus(status);
            assetRepository.save(asset);
            logAssetHistory(asset.getId(), "ASSET_UNASSIGNED", "Unassigned from customer ID: " + customerId, userId);
        }
    }

    @Transactional
    public AssetResponse updateAssetStatus(Long id, AssetStatus newStatus, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException("Asset with ID " + id + " not found."));

        AssetStatus oldStatus = asset.getAssetStatus();
        if (oldStatus != newStatus) {
            asset.setAssetStatus(newStatus);
            Asset updatedAsset = assetRepository.save(asset);
            logAssetHistory(updatedAsset.getId(), "STATUS_UPDATE", "Status changed from " + oldStatus + " to " + newStatus, userId);
            return mapToAssetResponse(updatedAsset);
        }
        return mapToAssetResponse(asset);
    }

    public List<AssetHistoryResponse> getAssetHistory(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new AssetNotFoundException("Asset with ID " + id + " not found.");
        }
        List<AssetHistory> history = assetHistoryRepository.findByAssetIdOrderByTimestampDesc(id);
        return history.stream().map(this::mapToAssetHistoryResponse).collect(Collectors.toList());
    }

    private void logAssetHistory(Long assetId, String changeType, String description, Long changedByUserId) {
        AssetHistory history = new AssetHistory();
        history.setAssetId(assetId);
        history.setChangeType(changeType);
        history.setDescription(description);
        history.setTimestamp(Instant.now());
        history.setChangedByUserId(changedByUserId);
        assetHistoryRepository.save(history);
    }

    private AssetResponse mapToAssetResponse(Asset asset) {
        AssetResponse response = new AssetResponse();
        response.setId(asset.getId());
        response.setSerialNumber(asset.getSerialNumber());
        response.setAssetType(asset.getAssetType());
        response.setModel(asset.getModel());
        response.setAssetStatus(asset.getAssetStatus());
        response.setLocation(asset.getLocation());
        response.setAssignedToCustomerId(asset.getAssignedToCustomerId());
        response.setCreatedAt(asset.getCreatedAt());
        return response;
    }

    private AssetHistoryResponse mapToAssetHistoryResponse(AssetHistory history) {
        AssetHistoryResponse response = new AssetHistoryResponse();
        response.setId(history.getId());
        response.setAssetId(history.getAssetId());
        response.setChangeType(history.getChangeType());
        response.setDescription(history.getDescription());
        response.setTimestamp(history.getTimestamp());
        response.setChangedByUserId(history.getChangedByUserId());
        return response;
    }
}
