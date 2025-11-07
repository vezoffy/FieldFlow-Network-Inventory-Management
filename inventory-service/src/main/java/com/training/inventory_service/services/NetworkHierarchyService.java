package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.entities.*;
import com.training.inventory_service.exceptions.AssetInUseException;
import com.training.inventory_service.exceptions.AssetNotFoundException;
import com.training.inventory_service.repositories.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class NetworkHierarchyService implements NetworkHierarchyServiceInterface {

    // --- Sonar: Constants for repeated strings (S1192) ---
    private static final String ASSET_NOT_FOUND_ID = "Asset not found with ID: %d";
    private static final String HEADEND_NOT_FOUND_ID = "Headend not found with ID: %d";
    private static final String CORE_SWITCH_NOT_FOUND_ID = "Core Switch not found with ID: %d";
    private static final String FDH_NOT_FOUND_ID = "FDH not found with ID: %d";
    private static final String SPLITTER_NOT_FOUND_ID = "Splitter not found with ID: %d";

    private static final String HEADEND_DETAILS_NOT_FOUND = "Headend details not found";
    private static final String CORE_SWITCH_DETAILS_NOT_FOUND = "Core Switch details not found";
    private static final String FDH_DETAILS_NOT_FOUND = "FDH details not found";
    private static final String SPLITTER_DETAILS_NOT_FOUND = "Splitter details not found";
    private static final String SPLITTER_NOT_FOUND = "Splitter not found";

    private static final String ASSET_CREATE_HIERARCHY_FAIL = "Failed to create asset during hierarchy setup";
    private static final String MODEL_INFRASTRUCTURE = "Infrastructure";
    private static final String MODEL_CORE_INFRASTRUCTURE = "Core Infrastructure";

    private final AssetRepository assetRepository;
    private final HeadendRepository headendRepository;
    private final FdhRepository fdhRepository;
    private final SplitterRepository splitterRepository;
    private final CoreSwitchRepository coreSwitchRepository;
    private final AssetServiceInterface assetService;

    @Autowired
    public NetworkHierarchyService(AssetRepository assetRepository, HeadendRepository headendRepository, FdhRepository fdhRepository, SplitterRepository splitterRepository, CoreSwitchRepository coreSwitchRepository, AssetServiceInterface assetService) {
        this.assetRepository = assetRepository;
        this.headendRepository = headendRepository;
        this.fdhRepository = fdhRepository;
        this.splitterRepository = splitterRepository;
        this.coreSwitchRepository = coreSwitchRepository;
        this.assetService = assetService;
    }

    @Transactional
    public Object updateAsset(Long assetId, AssetUpdateRequest request) {
        Asset asset = assetRepository.findById(assetId)
                .orElseThrow(() -> new AssetNotFoundException(String.format(ASSET_NOT_FOUND_ID, assetId)));

        // Update common asset fields
        if (request.getLocation() != null) asset.setLocation(request.getLocation());
        if (request.getModel() != null) asset.setModel(request.getModel());
        assetRepository.save(asset);

        // Update specific infrastructure fields
        switch (asset.getAssetType()) {
            case HEADEND:
                Headend headend = headendRepository.findByAssetId(assetId).orElseThrow(() -> new AssetNotFoundException(HEADEND_DETAILS_NOT_FOUND));
                if (request.getName() != null) headend.setName(request.getName());
                return toHeadendDto(headendRepository.save(headend));
            case CORE_SWITCH:
                CoreSwitch coreSwitch = coreSwitchRepository.findByAssetId(assetId).orElseThrow(() -> new AssetNotFoundException(CORE_SWITCH_DETAILS_NOT_FOUND));
                if (request.getName() != null) coreSwitch.setName(request.getName());
                return toCoreSwitchDto(coreSwitchRepository.save(coreSwitch));
            case FDH:
                Fdh fdh = fdhRepository.findByAssetId(assetId).orElseThrow(() -> new AssetNotFoundException(FDH_DETAILS_NOT_FOUND));
                if (request.getName() != null) fdh.setName(request.getName());
                if (request.getRegion() != null) fdh.setRegion(request.getRegion());
                return toFdhDto(fdhRepository.save(fdh));
            case SPLITTER:
                Splitter splitter = splitterRepository.findByAssetId(assetId).orElseThrow(() -> new AssetNotFoundException(SPLITTER_DETAILS_NOT_FOUND));
                if (request.getNeighborhood() != null) splitter.setNeighborhood(request.getNeighborhood());
                if (request.getPortCapacity() != null) splitter.setPortCapacity(request.getPortCapacity());
                return toSplitterDto(splitterRepository.save(splitter));
            // --- Sonar: Grouped cases with same logic (S131) ---
            case ONT, ROUTER, FIBER_ROLL:
                return mapToAssetResponse(asset);
            default:
                throw new IllegalArgumentException("Unsupported asset type for update: " + asset.getAssetType());
        }
    }

    public List<HeadendDto> getAllHeadends() {
        return headendRepository.findAll().stream().map(this::toHeadendDto).toList();
    }

    public List<CoreSwitchDto> getAllCoreSwitches() {
        return coreSwitchRepository.findAll().stream().map(this::toCoreSwitchDto).toList();
    }

    public List<FdhDto> getAllFdhs() {
        return fdhRepository.findAll().stream().map(this::toFdhDto).toList();
    }

    public List<SplitterDto> getAllSplitters() {
        return splitterRepository.findAll().stream().map(this::toSplitterDto).toList();
    }

    @Transactional
    public CoreSwitchDto reparentCoreSwitch(Long coreSwitchId, Long newHeadendId) {
        CoreSwitch coreSwitch = coreSwitchRepository.findById(coreSwitchId)
                .orElseThrow(() -> new AssetNotFoundException(String.format(CORE_SWITCH_NOT_FOUND_ID, coreSwitchId)));
        if (!headendRepository.existsById(newHeadendId)) {
            throw new AssetNotFoundException(String.format(HEADEND_NOT_FOUND_ID, newHeadendId));
        }
        coreSwitch.setHeadendId(newHeadendId);
        return toCoreSwitchDto(coreSwitchRepository.save(coreSwitch));
    }

    @Transactional
    public FdhDto reparentFdh(Long fdhId, Long newCoreSwitchId) {
        Fdh fdh = fdhRepository.findById(fdhId)
                .orElseThrow(() -> new AssetNotFoundException(String.format(FDH_NOT_FOUND_ID, fdhId)));
        if (!coreSwitchRepository.existsById(newCoreSwitchId)) {
            throw new AssetNotFoundException(String.format(CORE_SWITCH_NOT_FOUND_ID, newCoreSwitchId));
        }
        fdh.setCoreSwitchId(newCoreSwitchId);
        return toFdhDto(fdhRepository.save(fdh));
    }

    @Transactional
    public SplitterDto reparentSplitter(Long splitterId, Long newFdhId) {
        Splitter splitter = splitterRepository.findById(splitterId)
                .orElseThrow(() -> new AssetNotFoundException(String.format(SPLITTER_NOT_FOUND_ID, splitterId)));
        if (!fdhRepository.existsById(newFdhId)) {
            throw new AssetNotFoundException(String.format(FDH_NOT_FOUND_ID, newFdhId));
        }
        splitter.setFdhId(newFdhId);
        return toSplitterDto(splitterRepository.save(splitter));
    }

    // --- Sonar: Helper to remove duplicated code from create... methods ---
    private Asset prepareAssetRequest(AssetCreateRequest request, String defaultModel, String serialPrefix) {
        String serialNumber = StringUtils.hasText(request.getSerialNumber())
                ? request.getSerialNumber()
                : (StringUtils.hasText(request.getName()) ? request.getName() : serialPrefix + System.currentTimeMillis());
        request.setSerialNumber(serialNumber);

        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : defaultModel;
        request.setModel(model);

        AssetResponse assetResponse = assetService.createAsset(request);
        return assetRepository.findById(assetResponse.getId())
                .orElseThrow(() -> new AssetNotFoundException(ASSET_CREATE_HIERARCHY_FAIL));
    }

    @Transactional
    public HeadendDto createHeadend(AssetCreateRequest request) {
        Asset asset = prepareAssetRequest(request, MODEL_INFRASTRUCTURE, "HEADEND-");

        Headend headend = new Headend();
        headend.setAsset(asset);
        headend.setName(request.getName());
        headend.setLocation(request.getLocation());
        return toHeadendDto(headendRepository.save(headend));
    }

    @Transactional
    public CoreSwitchDto createCoreSwitch(AssetCreateRequest request) {
        Asset asset = prepareAssetRequest(request, MODEL_CORE_INFRASTRUCTURE, "CS-");

        CoreSwitch coreSwitch = new CoreSwitch();
        coreSwitch.setAsset(asset);
        coreSwitch.setName(request.getName());
        coreSwitch.setLocation(request.getLocation());
        coreSwitch.setHeadendId(request.getHeadendId());
        return toCoreSwitchDto(coreSwitchRepository.save(coreSwitch));
    }

    @Transactional
    public FdhDto createFdh(AssetCreateRequest request) {
        Asset asset = prepareAssetRequest(request, MODEL_INFRASTRUCTURE, "FDH-");

        Fdh fdh = new Fdh();
        fdh.setAsset(asset);
        fdh.setName(request.getName());
        fdh.setRegion(request.getRegion());
        fdh.setCoreSwitchId(request.getCoreSwitchId());
        return toFdhDto(fdhRepository.save(fdh));
    }

    @Transactional
    public SplitterDto createSplitter(AssetCreateRequest request) {
        String defaultModel = (request.getPortCapacity() != null)
                ? request.getPortCapacity() + "-Port Splitter"
                : "Splitter";
        String serialPrefix = "SPLITTER-" + request.getFdhId() + "-";

        Asset asset = prepareAssetRequest(request, defaultModel, serialPrefix);

        Splitter splitter = new Splitter();
        splitter.setAsset(asset);
        splitter.setFdhId(request.getFdhId());
        splitter.setPortCapacity(request.getPortCapacity());
        splitter.setUsedPorts(0);
        splitter.setNeighborhood(request.getNeighborhood());
        return toSplitterDto(splitterRepository.save(splitter));
    }

    @Transactional
    public SplitterDto updateSplitterUsedPorts(Long id, SplitterUpdateRequest request) {
        Splitter splitter = splitterRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(SPLITTER_NOT_FOUND));
        splitter.setUsedPorts(request.getUsedPorts());
        return toSplitterDto(splitterRepository.save(splitter));
    }

    public HeadendDto getHeadendDetails(Long id) {
        Headend headend = headendRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(String.format(HEADEND_NOT_FOUND_ID, id)));
        return toHeadendDto(headend);
    }

    public CoreSwitchDto getCoreSwitchDetails(Long id) {
        CoreSwitch coreSwitch = coreSwitchRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(String.format(CORE_SWITCH_NOT_FOUND_ID, id)));
        return toCoreSwitchDto(coreSwitch);
    }

    public FdhDto getFdhDetails(Long id) {
        Fdh fdh = fdhRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(String.format(FDH_NOT_FOUND_ID, id)));
        return toFdhDto(fdh);
    }

    public SplitterDto getSplitterDetails(Long id) {
        Splitter splitter = splitterRepository.findById(id)
                .orElseThrow(() -> new AssetNotFoundException(String.format(SPLITTER_NOT_FOUND_ID, id)));
        return toSplitterDto(splitter);
    }

    public List<SplitterDto> getSplittersByFdh(Long fdhId) {
        return splitterRepository.findByFdhId(fdhId).stream().map(this::toSplitterDto).toList();
    }

    /**
     * --- Sonar: Refactored to fix N+1 performance issue ---
     * This method now loads all child data in bulk instead of in a loop.
     */
    @Transactional(readOnly = true)
    public HeadendTopologyDto getHeadendTopology(Long headendId) {
        Headend headend = headendRepository.findById(headendId)
                .orElseThrow(() -> new AssetNotFoundException(String.format(HEADEND_NOT_FOUND_ID, headendId)));

        // 1. Fetch all core switches for this headend
        List<CoreSwitch> coreSwitches = coreSwitchRepository.findAllByHeadendId(headendId);
        if (coreSwitches.isEmpty()) {
            return toHeadendTopologyDto(headend, Collections.emptyList()); // No children, return early
        }
        List<Long> coreSwitchIds = coreSwitches.stream().map(CoreSwitch::getId).toList();

        // 2. Fetch all FDHs for all core switches in one query
        List<Fdh> fdhs = fdhRepository.findAllByCoreSwitchIdIn(coreSwitchIds);
        Map<Long, List<Fdh>> fdhsByCoreSwitchId = fdhs.stream()
                .collect(Collectors.groupingBy(Fdh::getCoreSwitchId));

        // 3. Fetch all Splitters for all FDHs in one query
        List<Long> fdhIds = fdhs.stream().map(Fdh::getId).toList();
        if (fdhIds.isEmpty()) {
            // No FDHs, but we still need to build the CoreSwitch DTOs
            return toHeadendTopologyDto(headend, coreSwitches.stream()
                    .map(cs -> toCoreSwitchTopologyDto(cs, Collections.emptyList(), Collections.emptyMap()))
                    .toList());
        }

        List<Splitter> splitters = splitterRepository.findByFdhIdIn(fdhIds);
        Map<Long, List<Splitter>> splittersByFdhId = splitters.stream()
                .collect(Collectors.groupingBy(Splitter::getFdhId));

        // 4. Build the DTOs using the pre-fetched maps
        List<CoreSwitchTopologyDto> coreSwitchDtos = coreSwitches.stream()
                .map(cs -> toCoreSwitchTopologyDto(cs, fdhsByCoreSwitchId.getOrDefault(cs.getId(), Collections.emptyList()), splittersByFdhId))
                .toList();

        return toHeadendTopologyDto(headend, coreSwitchDtos);
    }

    // --- Mappers ---

    private HeadendDto toHeadendDto(Headend headend) {
        HeadendDto dto = new HeadendDto();
        dto.setId(headend.getId());
        dto.setName(headend.getName());
        dto.setLocation(headend.getLocation());
        if (headend.getAsset() != null) {
            dto.setSerialNumber(headend.getAsset().getSerialNumber());
            dto.setModel(headend.getAsset().getModel());
        }
        return dto;
    }

    private CoreSwitchDto toCoreSwitchDto(CoreSwitch coreSwitch) {
        CoreSwitchDto dto = new CoreSwitchDto();
        dto.setId(coreSwitch.getId());
        dto.setName(coreSwitch.getName());
        dto.setLocation(coreSwitch.getLocation());
        dto.setHeadendId(coreSwitch.getHeadendId());
        if (coreSwitch.getAsset() != null) {
            dto.setSerialNumber(coreSwitch.getAsset().getSerialNumber());
            dto.setModel(coreSwitch.getAsset().getModel());
        }
        return dto;
    }

    private FdhDto toFdhDto(Fdh fdh) {
        FdhDto dto = new FdhDto();
        dto.setId(fdh.getId());
        dto.setName(fdh.getName());
        dto.setRegion(fdh.getRegion());
        dto.setCoreSwitchId(fdh.getCoreSwitchId());
        if (fdh.getAsset() != null) {
            dto.setSerialNumber(fdh.getAsset().getSerialNumber());
            dto.setModel(fdh.getAsset().getModel());
        }
        return dto;
    }

    private SplitterDto toSplitterDto(Splitter splitter) {
        SplitterDto dto = new SplitterDto();
        dto.setId(splitter.getId());
        dto.setFdhId(splitter.getFdhId());
        dto.setPortCapacity(splitter.getPortCapacity());
        dto.setUsedPorts(splitter.getUsedPorts());
        dto.setNeighborhood(splitter.getNeighborhood());
        if (splitter.getAsset() != null) {
            dto.setSerialNumber(splitter.getAsset().getSerialNumber());
            dto.setModel(splitter.getAsset().getModel());
        }
        return dto;
    }

    // --- Topology Mappers (Refactored for N+1 Fix) ---

    private HeadendTopologyDto toHeadendTopologyDto(Headend headend, List<CoreSwitchTopologyDto> coreSwitches) {
        HeadendTopologyDto dto = new HeadendTopologyDto();
        dto.setId(headend.getId());
        dto.setName(headend.getName());
        dto.setLocation(headend.getLocation());
        dto.setCoreSwitches(coreSwitches); // Set the pre-built list
        return dto;
    }

    private CoreSwitchTopologyDto toCoreSwitchTopologyDto(CoreSwitch coreSwitch, List<Fdh> fdhs, Map<Long, List<Splitter>> splittersByFdhId) {
        CoreSwitchTopologyDto dto = new CoreSwitchTopologyDto();
        dto.setId(coreSwitch.getId());
        dto.setName(coreSwitch.getName());
        dto.setLocation(coreSwitch.getLocation());
        dto.setHeadendId(coreSwitch.getHeadendId());

        List<FdhTopologyDto> fdhDtos = fdhs.stream()
                .map(fdh -> toFdhTopologyDto(fdh, splittersByFdhId.getOrDefault(fdh.getId(), Collections.emptyList())))
                .toList();
        dto.setFdhs(fdhDtos);
        return dto;
    }

    private FdhTopologyDto toFdhTopologyDto(Fdh fdh, List<Splitter> splitters) {
        FdhTopologyDto dto = new FdhTopologyDto();
        dto.setId(fdh.getId());
        dto.setName(fdh.getName());
        dto.setRegion(fdh.getRegion());
        dto.setCoreSwitchId(fdh.getCoreSwitchId());

        List<SplitterDto> splitterDtos = splitters.stream()
                .map(this::toSplitterDto)
                .toList();
        dto.setSplitters(splitterDtos);
        return dto;
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
}