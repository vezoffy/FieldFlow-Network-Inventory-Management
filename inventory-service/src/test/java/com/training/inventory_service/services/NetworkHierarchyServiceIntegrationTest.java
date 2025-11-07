package com.training.inventory_service.services;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.entities.CoreSwitch;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.repositories.AssetRepository;
import com.training.inventory_service.repositories.CoreSwitchRepository;
import com.training.inventory_service.repositories.FdhRepository;
import com.training.inventory_service.repositories.HeadendRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class NetworkHierarchyServiceIntegrationTest {

    @Autowired
    private NetworkHierarchyService networkHierarchyService;

    @Autowired
    private AssetRepository assetRepository;

    @Autowired
    private HeadendRepository headendRepository;

    @Autowired
    private CoreSwitchRepository coreSwitchRepository;

    @Autowired
    private FdhRepository fdhRepository;

    @Test
    void testCreateHeadend_Success() {
        AssetCreateRequest request = new AssetCreateRequest();
        request.setAssetType(AssetType.HEADEND);
        request.setName("Test Headend");
        request.setLocation("Test Location");

        HeadendDto result = networkHierarchyService.createHeadend(request);

        assertNotNull(result.getId());
        assertEquals("Test Headend", result.getName());

        // Verify that a corresponding asset was also created
        Asset asset = assetRepository.findBySerialNumber(result.getSerialNumber()).orElse(null);
        assertNotNull(asset);
        assertEquals(AssetType.HEADEND, asset.getAssetType());
    }

    @Test
    void testCreateCoreSwitch_And_ReparentCoreSwitch() {
        // 1. First, create parent Headends
        AssetCreateRequest headendReq1 = new AssetCreateRequest();
        headendReq1.setAssetType(AssetType.HEADEND);
        headendReq1.setName("Headend-1");
        headendReq1.setLocation("Location-1");
        HeadendDto headend1 = networkHierarchyService.createHeadend(headendReq1);

        AssetCreateRequest headendReq2 = new AssetCreateRequest();
        headendReq2.setAssetType(AssetType.HEADEND);
        headendReq2.setName("Headend-2");
        headendReq2.setLocation("Location-2");
        HeadendDto headend2 = networkHierarchyService.createHeadend(headendReq2);

        // 2. Create the Core Switch under the first headend
        AssetCreateRequest csRequest = new AssetCreateRequest();
        csRequest.setAssetType(AssetType.CORE_SWITCH);
        csRequest.setName("Test-CS-01");
        csRequest.setHeadendId(headend1.getId());

        CoreSwitchDto createdCoreSwitch = networkHierarchyService.createCoreSwitch(csRequest);
        assertEquals(headend1.getId(), createdCoreSwitch.getHeadendId());

        // 3. Reparent the Core Switch to the second headend
        CoreSwitchDto reparentedCoreSwitch = networkHierarchyService.reparentCoreSwitch(createdCoreSwitch.getId(), headend2.getId());
        assertEquals(headend2.getId(), reparentedCoreSwitch.getHeadendId());

        // 4. Verify the change in the database
        CoreSwitch finalCoreSwitch = coreSwitchRepository.findById(createdCoreSwitch.getId()).get();
        assertEquals(headend2.getId(), finalCoreSwitch.getHeadendId());
    }

    @Test
    void testGetHeadendTopology_ReturnsCorrectStructure() {
        // Arrange: Build a complete hierarchy
        AssetCreateRequest headendReq = new AssetCreateRequest();
        headendReq.setAssetType(AssetType.HEADEND);
        headendReq.setName("Topo-Headend");
        headendReq.setLocation("Location");
        HeadendDto headend = networkHierarchyService.createHeadend(headendReq);

        AssetCreateRequest csReq = new AssetCreateRequest();
        csReq.setAssetType(AssetType.CORE_SWITCH);
        csReq.setName("Topo-CS");
        csReq.setHeadendId(headend.getId());
        CoreSwitchDto coreSwitch = networkHierarchyService.createCoreSwitch(csReq);

        AssetCreateRequest fdhReq = new AssetCreateRequest();
        fdhReq.setAssetType(AssetType.FDH);
        fdhReq.setName("Topo-FDH");
        fdhReq.setCoreSwitchId(coreSwitch.getId());
        FdhDto fdh = networkHierarchyService.createFdh(fdhReq);

        AssetCreateRequest splitterReq = new AssetCreateRequest();
        splitterReq.setAssetType(AssetType.SPLITTER);
        splitterReq.setFdhId(fdh.getId());
        splitterReq.setPortCapacity(16);
        splitterReq.setNeighborhood("Neighborhood-A");
        SplitterDto splitter = networkHierarchyService.createSplitter(splitterReq);

        // Act: Get the topology
        HeadendTopologyDto topology = networkHierarchyService.getHeadendTopology(headend.getId());

        // Assert: Traverse the DTO to verify the structure
        assertNotNull(topology);
        assertEquals(headend.getName(), topology.getName());
        assertEquals(1, topology.getCoreSwitches().size());

        CoreSwitchTopologyDto csTopology = topology.getCoreSwitches().get(0);
        assertEquals(coreSwitch.getName(), csTopology.getName());
        assertEquals(1, csTopology.getFdhs().size());

        FdhTopologyDto fdhTopology = csTopology.getFdhs().get(0);
        assertEquals(fdh.getName(), fdhTopology.getName());
        assertEquals(1, fdhTopology.getSplitters().size());

        SplitterDto finalSplitter = fdhTopology.getSplitters().get(0);
        assertEquals(splitter.getSerialNumber(), finalSplitter.getSerialNumber());
        assertEquals(16, finalSplitter.getPortCapacity());
    }
}
