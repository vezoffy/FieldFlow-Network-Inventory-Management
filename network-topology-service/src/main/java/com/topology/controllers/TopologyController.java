package com.topology.controllers;

import com.topology.dto.CustomerPathResponse;
import com.topology.dto.FdhTopologyResponse;
import com.topology.dto.HeadendTopologyDto;
import com.topology.services.TopologyService;
import com.topology.services.TopologyServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/topology")
public class TopologyController {

    private final TopologyServiceInterface topologyService;

    @Autowired
    public TopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
    }

    @GetMapping("/customer/{customerId}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<CustomerPathResponse>> traceCustomerPath(@PathVariable Long customerId) {
        return topologyService.traceCustomerPath(customerId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/device/{serialNumber}")
    @PreAuthorize("isAuthenticated()")
    public Mono<ResponseEntity<Object>> traceDevice(@PathVariable String serialNumber) {
        return topologyService.traceDevicePath(serialNumber)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/fdh/{fdhId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public Mono<ResponseEntity<FdhTopologyResponse>> getFdhTopology(@PathVariable Long fdhId) {
        return topologyService.getFdhTopology(fdhId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @GetMapping("/headend/{headendId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public Mono<ResponseEntity<HeadendTopologyDto>> getHeadendTopology(@PathVariable Long headendId) {
        return topologyService.getHeadendTopology(headendId)
                .map(ResponseEntity::ok)
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }
}
