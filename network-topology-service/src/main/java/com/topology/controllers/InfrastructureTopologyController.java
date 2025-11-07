package com.topology.controllers;

import com.topology.dto.InfrastructurePathResponse;
import com.topology.services.TopologyService;
import com.topology.services.TopologyServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/topology/infrastructure")
public class InfrastructureTopologyController {

    private final TopologyServiceInterface topologyService;

    @Autowired
    public InfrastructureTopologyController(TopologyService topologyService) {
        this.topologyService = topologyService;
        }

    @GetMapping("/{serialNumber}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public Mono<InfrastructurePathResponse> traceInfrastructurePath(@PathVariable String serialNumber) {
        return topologyService.traceInfrastructurePath(serialNumber);
    }
}
