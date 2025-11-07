package com.training.inventory_service.controllers;

import com.training.inventory_service.dtos.*;
import com.training.inventory_service.services.NetworkHierarchyService;
import com.training.inventory_service.services.NetworkHierarchyServiceInterface;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class NetworkHierarchyController {

    private final NetworkHierarchyServiceInterface networkHierarchyService;

    @Autowired
    public NetworkHierarchyController(NetworkHierarchyService networkHierarchyService) {
        this.networkHierarchyService = networkHierarchyService;
    }
    // --- List All Endpoints ---

    @GetMapping("/headends")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<List<HeadendDto>> getAllHeadends() {
        return ResponseEntity.ok(networkHierarchyService.getAllHeadends());
    }

    @GetMapping("/core-switches")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<List<CoreSwitchDto>> getAllCoreSwitches() {
        return ResponseEntity.ok(networkHierarchyService.getAllCoreSwitches());
    }

    @GetMapping("/fdhs")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<List<FdhDto>> getAllFdhs() {
        return ResponseEntity.ok(networkHierarchyService.getAllFdhs());
    }

    @GetMapping("/splitters")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<List<SplitterDto>> getAllSplitters() {
        return ResponseEntity.ok(networkHierarchyService.getAllSplitters());
    }

    // --- Reparenting Endpoints ---

    @PatchMapping("/core-switches/{id}/reparent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<CoreSwitchDto> reparentCoreSwitch(@PathVariable Long id, @Valid @RequestBody CoreSwitchReparentRequest request) {
        return ResponseEntity.ok(networkHierarchyService.reparentCoreSwitch(id, request.getNewHeadendId()));
    }

    @PatchMapping("/fdhs/{id}/reparent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<FdhDto> reparentFdh(@PathVariable Long id, @Valid @RequestBody FdhReparentRequest request) {
        return ResponseEntity.ok(networkHierarchyService.reparentFdh(id, request.getNewCoreSwitchId()));
    }

    @PatchMapping("/splitters/{id}/reparent")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<SplitterDto> reparentSplitter(@PathVariable Long id, @Valid @RequestBody SplitterReparentRequest request) {
        return ResponseEntity.ok(networkHierarchyService.reparentSplitter(id, request.getNewFdhId()));
    }

    // --- Existing Endpoints ---

    @GetMapping("/headends/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<HeadendDto> getHeadendDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getHeadendDetails(id));
    }

    @GetMapping("/headends/{id}/topology")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<HeadendTopologyDto> getHeadendTopology(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getHeadendTopology(id));
    }

    @GetMapping("/core-switches/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<CoreSwitchDto> getCoreSwitchDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getCoreSwitchDetails(id));
    }

    @GetMapping("/fdhs/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<FdhDto> getFdhDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getFdhDetails(id));
    }

    @GetMapping("/splitters/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<SplitterDto> getSplitterDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getSplitterDetails(id));
    }

    @GetMapping("/fdhs/{id}/splitters")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<List<SplitterDto>> getSplittersByFdh(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getSplittersByFdh(id));
    }

    @PatchMapping("/splitters/{id}/used-ports")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'TECHNICIAN')")
    public ResponseEntity<SplitterDto> updateSplitterUsedPorts(@PathVariable Long id, @Valid @RequestBody SplitterUpdateRequest request) {
        return ResponseEntity.ok(networkHierarchyService.updateSplitterUsedPorts(id, request));
    }
}