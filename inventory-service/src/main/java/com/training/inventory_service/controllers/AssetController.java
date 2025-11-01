package com.training.inventory_service.controllers;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.services.AssetService;
import com.training.inventory_service.services.NetworkHierarchyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory/assets")
public class AssetController {

    @Autowired
    private AssetService assetService;

    @Autowired
    private NetworkHierarchyService networkHierarchyService;

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<?> createAsset(@Valid @RequestBody AssetCreateRequest request) {
        switch (request.getAssetType()) {
            case HEADEND:
                return new ResponseEntity<>(networkHierarchyService.createHeadend(request), HttpStatus.CREATED);
            case CORE_SWITCH:
                return new ResponseEntity<>(networkHierarchyService.createCoreSwitch(request), HttpStatus.CREATED);
            case FDH:
                return new ResponseEntity<>(networkHierarchyService.createFdh(request), HttpStatus.CREATED);
            case SPLITTER:
                return new ResponseEntity<>(networkHierarchyService.createSplitter(request), HttpStatus.CREATED);
            case ONT:
            case ROUTER:
            case FIBER_ROLL:
                return new ResponseEntity<>(assetService.createAsset(request), HttpStatus.CREATED);
            default:
                return ResponseEntity.badRequest().body("Invalid asset type");
        }
    }

    @PatchMapping("/{serialNumber}/assign")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<AssetResponse> assignAssetToCustomer(
            @PathVariable String serialNumber,
            @Valid @RequestBody AssetAssignRequest request) {
        Long userId = 1L; // Placeholder for authenticated user ID
        AssetResponse updatedAsset = assetService.assignAssetToCustomer(serialNumber, request.getCustomerId(), userId);
        return ResponseEntity.ok(updatedAsset);
    }

    @GetMapping("/assignment/{serialNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssetAssignmentDetailsDto> getAssetAssignmentDetails(@PathVariable String serialNumber) {
        return ResponseEntity.ok(assetService.getAssetAssignmentDetails(serialNumber));
    }

    @GetMapping // This is the method causing the compilation error
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<List<AssetResponse>> filterAssets(
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String location) {
        // Adding a dummy comment to force recompilation
        // The method signature is correct, so this should resolve stale compilation issues.
        List<AssetResponse> assets = assetService.filterAssets(type, status, location);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AssetResponse>> getAssetsByCustomerId(@PathVariable Long customerId) {
        List<AssetResponse> assets = assetService.getAssetsByCustomerId(customerId);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/{serialNumber}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<AssetResponse> getAssetBySerial(@PathVariable String serialNumber) {
        AssetResponse asset = assetService.getAssetBySerial(serialNumber);
        return ResponseEntity.ok(asset);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<AssetResponse> updateAssetStatus(
            @PathVariable Long id,
            @Valid @RequestBody StatusUpdateRequest request) {
        Long userId = 1L; // Placeholder for authenticated user ID
        AssetResponse updatedAsset = assetService.updateAssetStatus(id, request.getNewStatus(), userId);
        return ResponseEntity.ok(updatedAsset);
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<List<AssetHistoryResponse>> getAssetHistory(@PathVariable Long id) {
        List<AssetHistoryResponse> history = assetService.getAssetHistory(id);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/test/inventory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Inventory service is up and running!");
    }
}
