package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.entities.AssetHistory;
import com.training.inventory_service.entities.Splitter;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.exceptions.AssetAlreadyExistsException;
import com.training.inventory_service.exceptions.AssetInUseException;
import com.training.inventory_service.exceptions.AssetNotFoundException;
import com.training.inventory_service.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification; // Import for Specification
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;

@Service
public class AssetService implements AssetServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(AssetService.class);

    // --- Sonar: Use private final fields with constructor injection ---
    private final AssetRepository assetRepository;
    private final AssetHistoryRepository assetHistoryRepository;
    private final HeadendRepository headendRepository;
    private final CoreSwitchRepository coreSwitchRepository;
    private final FdhRepository fdhRepository;
    private final SplitterRepository splitterRepository;

    // --- Sonar: Exception Message Constants ---
    private static final String ASSET_NOT_FOUND_SERIAL_MSG = "Asset not found with serial number: %s";
    private static final String ASSET_NOT_FOUND_ID_MSG = "Asset not found with ID: %d";
    private static final String FAULTY_ASSET_NOT_FOUND_MSG = "Faulty asset with serial number %s not found.";
    private static final String NEW_ASSET_NOT_FOUND_MSG = "New asset with serial number %s not found.";
    private static final String ASSET_NOT_ASSIGNED_MSG = "Asset %s is not assigned to a customer and cannot be replaced.";
    private static final String REPLACEMENT_ASSET_NOT_AVAILABLE_MSG = "Replacement asset %s is not AVAILABLE.";
    private static final String ASSET_TYPES_DO_NOT_MATCH_MSG = "Asset types do not match. Cannot replace %s with %s";
    private static final String ASSET_NOT_ASSIGNED_SERIAL_MSG = "Asset with serial number: %s is not currently assigned to any customer.";
    private static final String CANNOT_DELETE_ASSIGNED_ASSET_MSG = "Cannot delete asset with ID %d. It is currently assigned to a customer.";
    private static final String CANNOT_DELETE_HEADEND_MSG = "Cannot delete Headend with ID %d. It has child Core Switches.";
    private static final String CANNOT_DELETE_CORE_SWITCH_MSG = "Cannot delete Core Switch with ID %d. It has child FDHs.";
    private static final String CANNOT_DELETE_FDH_MSG = "Cannot delete FDH with ID %d. It has child Splitters.";
    private static final String CANNOT_DELETE_SPLITTER_MSG = "Cannot delete Splitter with ID %d. It has active customer connections.";
    private static final String SPLITTER_DETAILS_NOT_FOUND_MSG = "Splitter details not found for asset ID: %d";
    private static final String ASSET_ALREADY_EXISTS_SERIAL_MSG = "Asset with serial number %s already exists.";

    // --- Sonar: Log Type Constants ---
    private static final String LOG_ASSET_REPLACEMENT_FAULTY = "ASSET_REPLACEMENT_FAULTY";
    private static final String LOG_ASSET_REPLACEMENT_NEW = "ASSET_REPLACEMENT_NEW";
    private static final String LOG_ASSET_UNASSIGNED = "ASSET_UNASSIGNED";
    private static final String LOG_ASSET_ASSIGNED = "ASSET_ASSIGNED";
    private static final String LOG_ASSET_CREATED = "ASSET_CREATED";
    private static final String LOG_STATUS_UPDATE = "STATUS_UPDATE";

    // --- Sonar: Log Description Format Constants ---
    private static final String LOG_DESC_REPLACED_FAULTY = "Marked as FAULTY and unassigned from customer ID: %d. Replaced by %s";
    private static final String LOG_DESC_REPLACED_NEW = "Assigned to customer ID: %d as replacement for %s";
    private static final String LOG_DESC_UNASSIGNED = "Unassigned from customer ID: %d";
    private static final String LOG_DESC_ASSIGNED = "Assigned to customer ID: %d";
    private static final String LOG_DESC_STATUS_UPDATE = "Status changed from %s to %s";
    private static final String LOG_DESC_ASSET_CREATED = "New asset created.";


    @Autowired
    public AssetService(AssetRepository assetRepository,
                        AssetHistoryRepository assetHistoryRepository,
                        HeadendRepository headendRepository,
                        CoreSwitchRepository coreSwitchRepository,
                        FdhRepository fdhRepository,
                        SplitterRepository splitterRepository) {
        this.assetRepository = assetRepository;
        this.assetHistoryRepository = assetHistoryRepository;
        this.headendRepository = headendRepository;
        this.coreSwitchRepository = coreSwitchRepository;
        this.fdhRepository = fdhRepository;
        this.splitterRepository = splitterRepository;
    }

    public List<AssetResponse> getFaultyAssignedAssets() {
        List<AssetType> types = List.of(AssetType.ONT, AssetType.ROUTER);
        List<Asset> assets = assetRepository.findByStatusAndAssignedAndType(AssetStatus.FAULTY, types);
        // Sonar: Using .toList() is compliant
        return assets.stream().map(this::mapToAssetResponse).toList();
    }

    @Transactional
    public AssetResponse replaceFaultyAsset(AssetReplacementRequest request, Long userId) {
        // 1. Find the faulty asset and validate it
        Asset faultyAsset = assetRepository.findBySerialNumber(request.getFaultyAssetSerialNumber())
                .orElseThrow(() -> new AssetNotFoundException(String.format(FAULTY_ASSET_NOT_FOUND_MSG, request.getFaultyAssetSerialNumber())));

        if (faultyAsset.getAssignedToCustomerId() == null) {
            throw new AssetInUseException(String.format(ASSET_NOT_ASSIGNED_MSG, faultyAsset.getSerialNumber()));
        }
        Long customerId = faultyAsset.getAssignedToCustomerId();

        // 2. Find the new asset and validate it
        Asset newAsset = assetRepository.findBySerialNumber(request.getNewAssetSerialNumber())
                .orElseThrow(() -> new AssetNotFoundException(String.format(NEW_ASSET_NOT_FOUND_MSG, request.getNewAssetSerialNumber())));

        if (newAsset.getAssetStatus() != AssetStatus.AVAILABLE) {
            throw new AssetInUseException(String.format(REPLACEMENT_ASSET_NOT_AVAILABLE_MSG, newAsset.getSerialNumber()));
        }

        if (faultyAsset.getAssetType() != newAsset.getAssetType()) {
            throw new IllegalArgumentException(String.format(ASSET_TYPES_DO_NOT_MATCH_MSG, faultyAsset.getAssetType(), newAsset.getAssetType()));
        }

        // 3. Update the faulty asset
        faultyAsset.setAssignedToCustomerId(null);
        faultyAsset.setAssetStatus(AssetStatus.FAULTY);
        assetRepository.save(faultyAsset);
        String logDescFaulty = String.format(LOG_DESC_REPLACED_FAULTY, customerId, newAsset.getSerialNumber());
        logAssetHistory(faultyAsset.getId(), LOG_ASSET_REPLACEMENT_FAULTY, logDescFaulty, userId);

        // 4. Update the new asset
        newAsset.setAssignedToCustomerId(customerId);
        newAsset.setAssetStatus(AssetStatus.ASSIGNED);
        Asset savedNewAsset = assetRepository.save(newAsset);
        String logDescNew = String.format(LOG_DESC_REPLACED_NEW, customerId, faultyAsset.getSerialNumber());
        logAssetHistory(newAsset.getId(), LOG_ASSET_REPLACEMENT_NEW, logDescNew, userId);

        logger.info("Successfully replaced faulty asset {} with new asset {}", faultyAsset.getSerialNumber(), newAsset.getSerialNumber());

        return mapToAssetResponse(savedNewAsset);
    }

    @Transactional
    public AssetResponse unassignCustomerAssetsBySerialNumber(String serialNumber) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_SERIAL_MSG, serialNumber)));

        if (asset.getAssignedToCustomerId() == null) {
            throw new AssetNotFoundException(String.format(ASSET_NOT_ASSIGNED_SERIAL_MSG, serialNumber));
        }

        Long customerId = asset.getAssignedToCustomerId();
        asset.setAssignedToCustomerId(null);
        asset.setAssetStatus(AssetStatus.AVAILABLE);
        Asset updatedAsset = assetRepository.save(asset);

        logAssetHistory(updatedAsset.getId(), LOG_ASSET_UNASSIGNED, String.format(LOG_DESC_UNASSIGNED, customerId), null);

        return mapToAssetResponse(updatedAsset);
    }

    @Transactional
    public void deleteAsset(Long assetId) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_ID_MSG, assetId)));

        if (asset.getAssetStatus() == AssetStatus.ASSIGNED) {
            throw new AssetInUseException(String.format(CANNOT_DELETE_ASSIGNED_ASSET_MSG, assetId));
        }

        // Sonar: Add default case and handle all enum types
        switch (asset.getAssetType()) {
            case HEADEND:
                if (coreSwitchRepository.existsByHeadendId(asset.getId())) {
                    throw new AssetInUseException(String.format(CANNOT_DELETE_HEADEND_MSG, assetId));
                }
                headendRepository.deleteByAssetId(assetId);
                break;
            case CORE_SWITCH:
                if (fdhRepository.existsByCoreSwitchId(asset.getId())) {
                    throw new AssetInUseException(String.format(CANNOT_DELETE_CORE_SWITCH_MSG, assetId));
                }
                coreSwitchRepository.deleteByAssetId(assetId);
                break;
            case FDH:
                if (splitterRepository.existsByFdhId(asset.getId())) {
                    throw new AssetInUseException(String.format(CANNOT_DELETE_FDH_MSG, assetId));
                }
                fdhRepository.deleteByAssetId(assetId);
                break;
            case SPLITTER:
                Splitter splitter = splitterRepository.findByAssetId(assetId)
                        .orElseThrow(() -> new AssetNotFoundException(String.format(SPLITTER_DETAILS_NOT_FOUND_MSG, assetId)));
                if (splitter.getUsedPorts() > 0) {
                    throw new AssetInUseException(String.format(CANNOT_DELETE_SPLITTER_MSG, assetId));
                }
                splitterRepository.deleteByAssetId(assetId);
                break;
            case ONT, ROUTER:
                // These are leaf nodes, no child dependency checks needed.
                break;
            default:
                // Log a warning if a new unhandled enum type appears
                logger.warn("No specific delete logic for asset type {}, proceeding with generic delete.", asset.getAssetType());
                break;
        }

        assetHistoryRepository.deleteByAssetId(assetId);
        assetRepository.delete(asset);
        logger.info("Successfully deleted asset with ID {}", assetId);
    }

    @Transactional
    public AssetResponse createAsset(AssetCreateRequest request) {
        if (request.getSerialNumber() != null && assetRepository.existsBySerialNumber(request.getSerialNumber())) {
            throw new AssetAlreadyExistsException(String.format(ASSET_ALREADY_EXISTS_SERIAL_MSG, request.getSerialNumber()));
        }

        Asset asset = new Asset();
        asset.setAssetType(request.getAssetType());
        asset.setSerialNumber(request.getSerialNumber());
        asset.setModel(request.getModel());
        asset.setLocation(request.getLocation());
        asset.setAssetStatus(AssetStatus.AVAILABLE);
        asset.setCreatedAt(Instant.now());

        Asset savedAsset = assetRepository.save(asset);

        logAssetHistory(savedAsset.getId(), LOG_ASSET_CREATED, LOG_DESC_ASSET_CREATED, null);

        return mapToAssetResponse(savedAsset);
    }

    public AssetResponse getAssetBySerial(String serialNumber) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_SERIAL_MSG, serialNumber)));
        return mapToAssetResponse(asset);
    }

    public AssetAssignmentDetailsDto getAssetAssignmentDetails(String serialNumber) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_SERIAL_MSG, serialNumber)));

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
        // Sonar: Use .toList() instead of .collect(Collectors.toList())
        return assets.stream().map(this::mapToAssetResponse).toList();
    }

    /**
     * Sonar: Refactored to use Specification API.
     * This is much more efficient as it filters in the database,
     * not in Java memory after a findAll().
     * NOTE: Your AssetRepository must extend JpaSpecificationExecutor<Asset, Long> for this to work.
     */
    public List<AssetResponse> filterAssets(AssetType type, AssetStatus status, String location) {

        //FIX: Start with a "conjunction" (an always-true predicate) instead of where(null)
        Specification<Asset> spec = (root, query, cb) -> cb.conjunction();

        if (type != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assetType"), type));
        }
        if (status != null) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("assetStatus"), status));
        }
        if (location != null && !location.isBlank()) {
            spec = spec.and((root, query, cb) -> cb.equal(root.get("location"), location));
        }

        // This will now work because your repository extends JpaSpecificationExecutor
        List<Asset> assets = assetRepository.findAll(spec);

        return assets.stream().map(this::mapToAssetResponse).toList();
    }

    @Transactional
    public AssetResponse assignAssetToCustomer(String serialNumber, Long customerId, Long userId) {
        Asset asset = assetRepository.findBySerialNumber(serialNumber)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_SERIAL_MSG, serialNumber)));

        asset.setAssignedToCustomerId(customerId);
        asset.setAssetStatus(AssetStatus.ASSIGNED);
        Asset updatedAsset = assetRepository.save(asset);

        logAssetHistory(updatedAsset.getId(), LOG_ASSET_ASSIGNED, String.format(LOG_DESC_ASSIGNED, customerId), userId);

        return mapToAssetResponse(updatedAsset);
    }

    @Transactional
    public void unassignAssetsFromCustomer(Long customerId, String newStatus, Long userId) {
        List<Asset> assets = assetRepository.findByAssignedToCustomerId(customerId);
        AssetStatus status = AssetStatus.valueOf(newStatus.toUpperCase());
        String logDesc = String.format(LOG_DESC_UNASSIGNED, customerId);

        for (Asset asset : assets) {
            asset.setAssignedToCustomerId(null);
            asset.setAssetStatus(status);
            assetRepository.save(asset);
            logAssetHistory(asset.getId(), LOG_ASSET_UNASSIGNED, logDesc, userId);
        }
    }

    @Transactional
    public AssetResponse updateAssetStatus(Long id, AssetStatus newStatus, Long userId) {
        Asset asset = assetRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_ID_MSG, id)));

        AssetStatus oldStatus = asset.getAssetStatus();
        if (oldStatus != newStatus) {
            asset.setAssetStatus(newStatus);
            Asset updatedAsset = assetRepository.save(asset);
            String logDesc = String.format(LOG_DESC_STATUS_UPDATE, oldStatus, newStatus);
            logAssetHistory(updatedAsset.getId(), LOG_STATUS_UPDATE, logDesc, userId);
            return mapToAssetResponse(updatedAsset);
        }
        return mapToAssetResponse(asset);
    }

    public List<AssetHistoryResponse> getAssetHistory(Long id) {
        if (!assetRepository.existsById(id)) {
            throw new AssetNotFoundException(String.format(ASSET_NOT_FOUND_ID_MSG, id));
        }
        List<AssetHistory> history = assetHistoryRepository.findByAssetIdOrderByTimestampDesc(id);
        // Sonar: Use .toList()
        return history.stream().map(this::mapToAssetHistoryResponse).toList();
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

    // Sonar: This is a helper method and should be private
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