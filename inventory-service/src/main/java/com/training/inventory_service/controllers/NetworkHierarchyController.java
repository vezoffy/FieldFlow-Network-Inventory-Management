package com.training.inventory_service.controllers;

import com.training.inventory_service.dtos.CoreSwitchDto;
import com.training.inventory_service.dtos.FdhDto;
import com.training.inventory_service.dtos.HeadendDto;
import com.training.inventory_service.dtos.HeadendTopologyDto;
import com.training.inventory_service.dtos.SplitterDto;
import com.training.inventory_service.dtos.SplitterUpdateRequest;
import com.training.inventory_service.services.NetworkHierarchyService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/inventory")
public class NetworkHierarchyController {

    @Autowired
    private NetworkHierarchyService networkHierarchyService;

    @GetMapping("/headends/{id}")
    public ResponseEntity<HeadendDto> getHeadendDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getHeadendDetails(id));
    }

    @GetMapping("/headends/{id}/topology")
    public ResponseEntity<HeadendTopologyDto> getHeadendTopology(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getHeadendTopology(id));
    }

    @GetMapping("/core-switches/{id}")
    public ResponseEntity<CoreSwitchDto> getCoreSwitchDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getCoreSwitchDetails(id));
    }

    @GetMapping("/fdhs/{id}")
    public ResponseEntity<FdhDto> getFdhDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getFdhDetails(id));
    }

    @GetMapping("/splitters/{id}")
    public ResponseEntity<SplitterDto> getSplitterDetails(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getSplitterDetails(id));
    }

    @GetMapping("/fdhs/{id}/splitters")
    public ResponseEntity<List<SplitterDto>> getSplittersByFdh(@PathVariable Long id) {
        return ResponseEntity.ok(networkHierarchyService.getSplittersByFdh(id));
    }

    @PatchMapping("/splitters/{id}/used-ports")
    public ResponseEntity<SplitterDto> updateSplitterUsedPorts(@PathVariable Long id, @Valid @RequestBody SplitterUpdateRequest request) {
        return ResponseEntity.ok(networkHierarchyService.updateSplitterUsedPorts(id, request));
    }
}
