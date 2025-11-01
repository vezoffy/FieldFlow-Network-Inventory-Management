package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.entities.AssetHistory;
import com.training.inventory_service.entities.CoreSwitch;
import com.training.inventory_service.entities.Fdh;
import com.training.inventory_service.entities.Headend;
import com.training.inventory_service.entities.Splitter;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.exceptions.AssetAlreadyExistsException;
import com.training.inventory_service.exceptions.AssetNotFoundException;
import com.training.inventory_service.repositories.AssetRepository;
import com.training.inventory_service.repositories.AssetHistoryRepository;
import com.training.inventory_service.repositories.CoreSwitchRepository;
import com.training.inventory_service.repositories.FdhRepository;
import com.training.inventory_service.repositories.HeadendRepository;
import com.training.inventory_service.repositories.SplitterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AssetService {

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private AssetHistoryRepository assetHistoryRepository;

    @Autowired
    private HeadendRepository headendRepository;

    @Autowired
    private CoreSwitchRepository coreSwitchRepository;

    @Autowired
    private FdhRepository fdhRepository;

    @Autowired
    private SplitterRepository splitterRepository;

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
        dto.setAssetId(asset.getId()); // Set the asset ID for the current device

        switch (asset.getAssetType()) {
            case ONT:
            case ROUTER:
            case FIBER_ROLL:
                if (asset.getAssignedToCustomerId() == null) {
                    throw new AssetNotFoundException("Asset " + serialNumber + " is not assigned to any customer.");
                }
                dto.setCustomerId(asset.getAssignedToCustomerId());
                break;
            case HEADEND:
                // Find the CoreSwitch linked to this Headend
                coreSwitchRepository.findByHeadendId(asset.getId()).ifPresent(cs -> {
                    dto.setNextDeviceId(cs.getId());
                    dto.setNextDeviceSerialNumber(cs.getAsset().getSerialNumber());
                });
                break;
            case CORE_SWITCH:
                // Find the FDH linked to this CoreSwitch
                fdhRepository.findByCoreSwitchId(asset.getId()).ifPresent(fdh -> {
                    dto.setNextDeviceId(fdh.getId());
                    dto.setNextDeviceSerialNumber(fdh.getAsset().getSerialNumber());
                });
                break;
            case FDH:
                // Find a Splitter linked to this FDH (we'll just return the first one for now)
                List<Splitter> splitters = splitterRepository.findByFdhId(asset.getId());
                if (!splitters.isEmpty()) {
                    Splitter firstSplitter = splitters.get(0);
                    dto.setNextDeviceId(firstSplitter.getId());
                    dto.setNextDeviceSerialNumber(firstSplitter.getAsset().getSerialNumber());
                }
                break;
            case SPLITTER:
                // Splitters are linked to customers, but we need the customer ID from customer-service
                // No nextDeviceId here, as it's a leaf node in the inventory hierarchy for tracing purposes
                // The network-topology-service will call customer-service to find customers by splitter ID
                break;
            default:
                // No next device for other types or end of path
                break;
        }
        return dto;
    }

    public List<AssetResponse> getAssetsByCustomerId(Long customerId) {
        List<Asset> assets = assetRepository.findByAssignedToCustomerId(customerId);
        return assets.stream().map(this::mapToAssetResponse).collect(Collectors.toList());
    }

    public List<AssetResponse> filterAssets(AssetType type, AssetStatus status, String location) {
        // Start with all assets
        List<Asset> assets = assetRepository.findAll();

        // Apply filters conditionally
        if (type != null) {
            assets = assets.stream()
                    .filter(asset -> asset.getAssetType().equals(type))
                    .collect(Collectors.toList());
        }
        if (status != null) {
            assets = assets.stream()
                    .filter(asset -> asset.getAssetStatus().equals(status))
                    .collect(Collectors.toList());
        }
        if (location != null && !location.isBlank()) {
            assets = assets.stream()
                    .filter(asset -> asset.getLocation() != null && asset.getLocation().equalsIgnoreCase(location))
                    .collect(Collectors.toList());
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
    public AssetResponse updateAssetDetails(Long id, AssetUpdateRequest request) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException("Asset with ID " + id + " not found."));

        String oldModel = asset.getModel();
        String oldLocation = asset.getLocation();

        if (request.getModel() != null && !request.getModel().isBlank()) {
            asset.setModel(request.getModel());
        }
        if (request.getLocation() != null && !request.getLocation().isBlank()) {
            asset.setLocation(request.getLocation());
        }

        Asset updatedAsset = assetRepository.save(asset);

        if (!oldModel.equals(asset.getModel()) || !oldLocation.equals(asset.getLocation())) {
            logAssetHistory(updatedAsset.getId(), "DETAILS_UPDATED", "Asset details updated.", null);
        }

        return mapToAssetResponse(updatedAsset);
    }

    @Transactional
    public AssetResponse updateAssetStatus(Long id, AssetStatus newStatus, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException("Asset with ID " + id + " not found."));

        AssetStatus oldStatus = asset.getAssetStatus();
        if (!oldStatus.equals(newStatus)) {
            asset.setAssetStatus(newStatus);
            Asset updatedAsset = assetRepository.save(asset);
            logAssetHistory(updatedAsset.getId(), "STATUS_UPDATE", "Status changed from " + oldStatus + " to " + newStatus + ".", userId);
            return mapToAssetResponse(updatedAsset);
        }
        return mapToAssetResponse(asset); // No change if status is the same
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
