
package com.training.inventory_service.services;

import com.training.inventory_service.dtos.AssetReplacementRequest;
import com.training.inventory_service.dtos.AssetResponse;
import com.training.inventory_service.entities.Asset;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.exceptions.AssetInUseException;
import com.training.inventory_service.exceptions.AssetNotFoundException;
import com.training.inventory_service.repositories.AssetRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional // Roll back transactions after each test
class AssetServiceIntegrationTest {

    @Autowired
    private AssetServiceInterface assetService;

    @Autowired
    private AssetRepository assetRepository;

    private Asset faultyOnt;
    private Asset newOnt;
    private Asset newRouter;

    @BeforeEach
    void setUp() {
        // Create a faulty asset assigned to a customer
        faultyOnt = new Asset();
        faultyOnt.setAssetType(AssetType.ONT);
        faultyOnt.setSerialNumber("FAULTY-ONT-123");
        faultyOnt.setAssetStatus(AssetStatus.FAULTY);
        faultyOnt.setAssignedToCustomerId(101L);
        assetRepository.save(faultyOnt);

        // Create an available replacement asset
        newOnt = new Asset();
        newOnt.setAssetType(AssetType.ONT);
        newOnt.setSerialNumber("NEW-ONT-456");
        newOnt.setAssetStatus(AssetStatus.AVAILABLE);
        assetRepository.save(newOnt);

        // Create an asset of a different type for failure testing
        newRouter = new Asset();
        newRouter.setAssetType(AssetType.ROUTER);
        newRouter.setSerialNumber("NEW-ROUTER-789");
        newRouter.setAssetStatus(AssetStatus.AVAILABLE);
        assetRepository.save(newRouter);
    }

    @Test
    void testReplaceFaultyAsset_Success() {
        AssetReplacementRequest request = new AssetReplacementRequest();
        request.setFaultyAssetSerialNumber("FAULTY-ONT-123");
        request.setNewAssetSerialNumber("NEW-ONT-456");

        // Execute the replacement
        AssetResponse result = assetService.replaceFaultyAsset(request, 1L); // userId = 1L for admin

        // Verify the new asset
        assertNotNull(result);
        assertEquals("NEW-ONT-456", result.getSerialNumber());
        assertEquals(AssetStatus.ASSIGNED, result.getAssetStatus());
        assertEquals(101L, result.getAssignedToCustomerId());

        // Verify the old asset
        Asset updatedFaultyAsset = assetRepository.findBySerialNumber("FAULTY-ONT-123").get();
        assertEquals(AssetStatus.FAULTY, updatedFaultyAsset.getAssetStatus());
        assertNull(updatedFaultyAsset.getAssignedToCustomerId());
    }

    @Test
    void testReplaceFaultyAsset_ThrowsException_WhenNewAssetNotAvailable() {
        newOnt.setAssetStatus(AssetStatus.ASSIGNED); // Make the new ONT unavailable
        assetRepository.save(newOnt);

        AssetReplacementRequest request = new AssetReplacementRequest();
        request.setFaultyAssetSerialNumber("FAULTY-ONT-123");
        request.setNewAssetSerialNumber("NEW-ONT-456");

        // Assert that the correct exception is thrown
        assertThrows(AssetInUseException.class, () -> {
            assetService.replaceFaultyAsset(request, 1L);
        });
    }

    @Test
    void testReplaceFaultyAsset_ThrowsException_WhenAssetTypesDoNotMatch() {
        AssetReplacementRequest request = new AssetReplacementRequest();
        request.setFaultyAssetSerialNumber("FAULTY-ONT-123");
        request.setNewAssetSerialNumber("NEW-ROUTER-789"); // Mismatched type

        assertThrows(IllegalArgumentException.class, () -> {
            assetService.replaceFaultyAsset(request, 1L);
        });
    }

    @Test
    void testReplaceFaultyAsset_ThrowsException_WhenFaultyAssetNotFound() {
        AssetReplacementRequest request = new AssetReplacementRequest();
        request.setFaultyAssetSerialNumber("NON-EXISTENT-SN");
        request.setNewAssetSerialNumber("NEW-ONT-456");

        assertThrows(AssetNotFoundException.class, () -> {
            assetService.replaceFaultyAsset(request, 1L);
        });
    }
}
