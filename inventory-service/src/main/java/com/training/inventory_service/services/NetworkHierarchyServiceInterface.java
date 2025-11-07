package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;

import java.util.List;

public interface NetworkHierarchyServiceInterface {
    Object updateAsset(Long assetId, AssetUpdateRequest request);

    List<HeadendDto> getAllHeadends();
    List<CoreSwitchDto> getAllCoreSwitches();

    List<FdhDto> getAllFdhs();
    List<SplitterDto> getAllSplitters();

    CoreSwitchDto reparentCoreSwitch(Long coreSwitchId, Long newHeadendId);
    FdhDto reparentFdh(Long fdhId, Long newCoreSwitchId);
    SplitterDto reparentSplitter(Long splitterId, Long newFdhId);

    HeadendDto createHeadend(AssetCreateRequest request);
    CoreSwitchDto createCoreSwitch(AssetCreateRequest request);
    FdhDto createFdh(AssetCreateRequest request);
    SplitterDto createSplitter(AssetCreateRequest request);

    SplitterDto updateSplitterUsedPorts(Long id, SplitterUpdateRequest request);
    HeadendDto getHeadendDetails(Long id);
    CoreSwitchDto getCoreSwitchDetails(Long id);
    FdhDto getFdhDetails(Long id);
    SplitterDto getSplitterDetails(Long id);
    List<SplitterDto> getSplittersByFdh(Long fdhId);

    HeadendTopologyDto getHeadendTopology(Long headendId);
}
