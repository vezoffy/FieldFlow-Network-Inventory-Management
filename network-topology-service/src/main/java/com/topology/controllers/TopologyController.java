package com.topology.controllers;

import com.topology.dto.CustomerPathResponse;
import com.topology.dto.FdhTopologyResponse;
import com.topology.dto.HeadendTopologyDto;
import com.topology.services.TopologyService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/topology")
public class TopologyController {

    @Autowired
    private TopologyService topologyService;

    @GetMapping("/headend/{headendId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public Mono<HeadendTopologyDto> getHeadendTopology(@PathVariable Long headendId) {
        return topologyService.getHeadendTopology(headendId);
    }

    // This endpoint now returns the specific hierarchical path for a customer
    @GetMapping("/customer/{customerId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public Mono<CustomerPathResponse> getCustomerTopology(@PathVariable Long customerId) {
        return topologyService.traceCustomerPath(customerId);
    }

    @GetMapping("/fdh/{fdhId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public Mono<FdhTopologyResponse> getFdhTopology(@PathVariable Long fdhId) {
        return topologyService.getFdhTopology(fdhId);
    }

    // This endpoint now also returns the specific hierarchical path for the customer assigned to the device
    @GetMapping("/search/device/{serial}")
    @PreAuthorize("isAuthenticated()")
    public Mono<CustomerPathResponse> traceDevicePath(@PathVariable String serial) {
        return topologyService.traceDevicePath(serial);
    }
}
