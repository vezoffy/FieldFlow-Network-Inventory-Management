package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.entities.CoreSwitch;
import com.training.inventory_service.entities.Fdh;
import com.training.inventory_service.entities.Headend;
import com.training.inventory_service.entities.Splitter;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.exceptions.AssetAlreadyExistsException;
import com.training.inventory_service.exceptions.AssetNotFoundException;
import com.training.inventory_service.repositories.AssetRepository;
import com.training.inventory_service.repositories.CoreSwitchRepository;
import com.training.inventory_service.repositories.FdhRepository;
import com.training.inventory_service.repositories.HeadendRepository;
import com.training.inventory_service.repositories.SplitterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class NetworkHierarchyService {

    private static final Logger logger = LoggerFactory.getLogger(NetworkHierarchyService.class);

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private HeadendRepository headendRepository;

    @Autowired
    private FdhRepository fdhRepository;

    @Autowired
    private SplitterRepository splitterRepository;

    @Autowired
    private CoreSwitchRepository coreSwitchRepository;

    @Autowired
    private AssetService assetService;

    // ... (create methods remain the same) ...

    @Transactional
    public HeadendDto createHeadend(AssetCreateRequest request) {
        String serialNumber = StringUtils.hasText(request.getSerialNumber()) ? request.getSerialNumber() : request.getName();
        request.setSerialNumber(serialNumber);
        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : "Infrastructure";
        request.setModel(model);

        AssetResponse assetResponse = assetService.createAsset(request);
        Asset asset = assetRepository.findById(assetResponse.getId()).orElseThrow(() -> new AssetNotFoundException("Failed to create asset during hierarchy setup"));

        Headend headend = new Headend();
        headend.setAsset(asset); // Set the parent object
        headend.setName(request.getName());
        headend.setLocation(request.getLocation());
        Headend savedHeadend = headendRepository.save(headend);
        return toHeadendDto(savedHeadend);
    }

    @Transactional
    public CoreSwitchDto createCoreSwitch(AssetCreateRequest request) {
        String serialNumber = StringUtils.hasText(request.getSerialNumber()) ? request.getSerialNumber() : request.getName();
        request.setSerialNumber(serialNumber);
        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : "Core Infrastructure";
        request.setModel(model);

        AssetResponse assetResponse = assetService.createAsset(request);
        Asset asset = assetRepository.findById(assetResponse.getId()).orElseThrow(() -> new AssetNotFoundException("Failed to create asset during hierarchy setup"));

        CoreSwitch coreSwitch = new CoreSwitch();
        coreSwitch.setAsset(asset);
        coreSwitch.setName(request.getName());
        coreSwitch.setLocation(request.getLocation());
        coreSwitch.setHeadendId(request.getHeadendId());
        CoreSwitch savedCoreSwitch = coreSwitchRepository.save(coreSwitch);
        return toCoreSwitchDto(savedCoreSwitch);
    }

    @Transactional
    public FdhDto createFdh(AssetCreateRequest request) {
        String serialNumber = StringUtils.hasText(request.getSerialNumber()) ? request.getSerialNumber() : request.getName();
        request.setSerialNumber(serialNumber);
        String model = StringUtils.hasText(request.getModel()) ? request.getModel() : "Infrastructure";
        request.setModel(model);

        AssetResponse assetResponse = assetService.createAsset(request);
        Asset asset = assetRepository.findById(assetResponse.getId()).orElseThrow(() -> new AssetNotFoundException("Failed to create asset during hierarchy setup"));

        Fdh fdh = new Fdh();
        fdh.setAsset(asset);
        fdh.setName(request.getName());
        fdh.setRegion(request.getRegion());
        fdh.setCoreSwitchId(request.getCoreSwitchId());
        Fdh savedFdh = fdhRepository.save(fdh);
        return toFdhDto(savedFdh);
    }

    @Transactional
    public SplitterDto createSplitter(AssetCreateRequest request) {
        String serialNumber = StringUtils.hasText(request.getSerialNumber()) 
            ? request.getSerialNumber() 
            : "SPLITTER-" + request.getFdhId() + "-" + System.currentTimeMillis();
        request.setSerialNumber(serialNumber);

        String model = StringUtils.hasText(request.getModel()) 
            ? request.getModel() 
            : request.getPortCapacity() + "-Port Splitter";
        request.setModel(model);

        AssetResponse assetResponse = assetService.createAsset(request);
        Asset asset = assetRepository.findById(assetResponse.getId()).orElseThrow(() -> new AssetNotFoundException("Failed to create asset during hierarchy setup"));

        Splitter splitter = new Splitter();
        splitter.setAsset(asset);
        splitter.setFdhId(request.getFdhId());
        splitter.setPortCapacity(request.getPortCapacity());
        splitter.setUsedPorts(0);
        splitter.setNeighborhood(request.getNeighborhood()); // Set the new field
        Splitter savedSplitter = splitterRepository.save(splitter);
        return toSplitterDto(savedSplitter);
    }

    @Transactional
    public SplitterDto updateSplitterUsedPorts(Long id, SplitterUpdateRequest request) {
        Splitter splitter = splitterRepository.findById(id).orElseThrow(() -> new AssetNotFoundException("Splitter not found"));
        splitter.setUsedPorts(request.getUsedPorts());
        return toSplitterDto(splitterRepository.save(splitter));
    }

    public HeadendDto getHeadendDetails(Long id) {
        Headend headend = headendRepository.findById(id).orElseThrow(() -> new AssetNotFoundException("Headend not found"));
        return toHeadendDto(headend);
    }

    public CoreSwitchDto getCoreSwitchDetails(Long id) {
        CoreSwitch coreSwitch = coreSwitchRepository.findById(id).orElseThrow(() -> new AssetNotFoundException("Core Switch not found"));
        return toCoreSwitchDto(coreSwitch);
    }

    public FdhDto getFdhDetails(Long id) {
        Fdh fdh = fdhRepository.findById(id).orElseThrow(() -> new AssetNotFoundException("FDH not found"));
        return toFdhDto(fdh);
    }

    public SplitterDto getSplitterDetails(Long id) {
        Splitter splitter = splitterRepository.findById(id).orElseThrow(() -> new AssetNotFoundException("Splitter not found"));
        return toSplitterDto(splitter);
    }

    public List<SplitterDto> getSplittersByFdh(Long fdhId) {
        return splitterRepository.findByFdhId(fdhId).stream().map(this::toSplitterDto).collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public HeadendTopologyDto getHeadendTopology(Long headendId) {
        Headend headend = headendRepository.findById(headendId)
                .orElseThrow(() -> new AssetNotFoundException("Headend not found with ID: " + headendId));
        HeadendTopologyDto headendDto = toHeadendTopologyDto(headend);

        List<CoreSwitchTopologyDto> coreSwitches = coreSwitchRepository.findAllByHeadendId(headendId).stream()
                .map(this::toCoreSwitchTopologyDto)
                .collect(Collectors.toList());
        headendDto.setCoreSwitches(coreSwitches);

        return headendDto;
    }

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
        }
        return dto;
    }

    private HeadendTopologyDto toHeadendTopologyDto(Headend headend) {
        HeadendTopologyDto dto = new HeadendTopologyDto();
        dto.setId(headend.getId());
        dto.setName(headend.getName());
        dto.setLocation(headend.getLocation());
        return dto;
    }

    private CoreSwitchTopologyDto toCoreSwitchTopologyDto(CoreSwitch coreSwitch) {
        CoreSwitchTopologyDto dto = new CoreSwitchTopologyDto();
        dto.setId(coreSwitch.getId());
        dto.setName(coreSwitch.getName());
        dto.setLocation(coreSwitch.getLocation());
        dto.setHeadendId(coreSwitch.getHeadendId());
        List<FdhTopologyDto> fdhs = fdhRepository.findAllByCoreSwitchId(coreSwitch.getId()).stream()
                .map(this::toFdhTopologyDto)
                .collect(Collectors.toList());
        dto.setFdhs(fdhs);
        return dto;
    }

    private FdhTopologyDto toFdhTopologyDto(Fdh fdh) {
        FdhTopologyDto dto = new FdhTopologyDto();
        dto.setId(fdh.getId());
        dto.setName(fdh.getName());
        dto.setRegion(fdh.getRegion());
        dto.setCoreSwitchId(fdh.getCoreSwitchId());
        List<SplitterDto> splitters = splitterRepository.findByFdhId(fdh.getId()).stream()
                .map(this::toSplitterDto)
                .collect(Collectors.toList());
        dto.setSplitters(splitters);
        return dto;
    }
}
