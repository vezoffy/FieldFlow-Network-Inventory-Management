package com.training.inventory_service.controllers;

import com.training.inventory_service.dtos.AssetReclaimRequest;
import com.training.inventory_service.services.AssetService;
import com.training.inventory_service.services.AssetServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/inventory/assets")
public class DeploymentIntegrationController {

    private final AssetServiceInterface assetService;

    @Autowired
    public DeploymentIntegrationController(AssetService assetService) {
        this.assetService = assetService;
    }

    @PatchMapping("/unassign/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')") // Secured for internal system calls
    public ResponseEntity<Void> reclaimAssetsByCustomer(@PathVariable Long customerId, @RequestBody AssetReclaimRequest request) {
        // The userId for logging can be extracted from JWT if needed, passing null for now
        assetService.unassignAssetsFromCustomer(customerId, request.getStatus(), null);
        return ResponseEntity.ok().build();
    }
}
