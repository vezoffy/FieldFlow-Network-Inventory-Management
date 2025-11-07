package com.training.inventory_service.controllers;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.enums.AssetStatus;
import com.training.inventory_service.enums.AssetType;
import com.training.inventory_service.services.AssetService;
import com.training.inventory_service.services.AssetServiceInterface;
import com.training.inventory_service.services.NetworkHierarchyService;
import com.training.inventory_service.services.NetworkHierarchyServiceInterface;
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

    private final AssetServiceInterface assetService;
    private final NetworkHierarchyServiceInterface networkHierarchyService;

    @Autowired
    public AssetController(AssetService assetService, NetworkHierarchyService networkHierarchyService) {
        this.assetService = assetService;
        this.networkHierarchyService = networkHierarchyService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<Object> createAsset(@Valid @RequestBody AssetCreateRequest request) {
        return switch (request.getAssetType()) {
            case HEADEND -> new ResponseEntity<>(networkHierarchyService.createHeadend(request), HttpStatus.CREATED);
            case CORE_SWITCH ->
                    new ResponseEntity<>(networkHierarchyService.createCoreSwitch(request), HttpStatus.CREATED);
            case FDH -> new ResponseEntity<>(networkHierarchyService.createFdh(request), HttpStatus.CREATED);
            case SPLITTER -> new ResponseEntity<>(networkHierarchyService.createSplitter(request), HttpStatus.CREATED);
            case ONT, ROUTER, FIBER_ROLL -> new ResponseEntity<>(assetService.createAsset(request), HttpStatus.CREATED);
            default -> ResponseEntity.badRequest().body("Invalid asset type");
        };
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Object> updateAsset(@PathVariable Long id, @Valid @RequestBody AssetUpdateRequest request) {
        return ResponseEntity.ok(networkHierarchyService.updateAsset(id, request));
    }

    @DeleteMapping("/by-id/{id}") // Corrected Path
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteAsset(@PathVariable Long id) {
        assetService.deleteAsset(id);
        return ResponseEntity.noContent().build();
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

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<List<AssetResponse>> filterAssets(
            @RequestParam(required = false) AssetType type,
            @RequestParam(required = false) AssetStatus status,
            @RequestParam(required = false) String location) {
        List<AssetResponse> assets = assetService.filterAssets(type, status, location);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<AssetResponse>> getAssetsByCustomerId(@PathVariable Long customerId) {
        List<AssetResponse> assets = assetService.getAssetsByCustomerId(customerId);
        return ResponseEntity.ok(assets);
    }

    @GetMapping("/by-serial/{serialNumber}") // Corrected Path
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

    @PatchMapping("/unassign/by-serial/{serialNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<AssetResponse> unassignAsset(@PathVariable String serialNumber) {
        AssetResponse updatedAsset = assetService.unassignCustomerAssetsBySerialNumber(serialNumber);
        return ResponseEntity.ok(updatedAsset);
    }

    @PostMapping("/replace")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_AGENT')")
    public ResponseEntity<AssetResponse> replaceFaultyAsset(@RequestBody AssetReplacementRequest request) {
        // In a real scenario, the userId would be extracted from the JWT principal
        AssetResponse newAsset = assetService.replaceFaultyAsset(request, null);
        return ResponseEntity.ok(newAsset);
    }

    @GetMapping("/faulty-assigned")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN', 'SUPPORT_AGENT')")
    public ResponseEntity<List<AssetResponse>> getFaultyAssignedAssets() {
        return ResponseEntity.ok(assetService.getFaultyAssignedAssets());
    }

    @GetMapping("/test/inventory")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Inventory service is up and running!");
    }
}
