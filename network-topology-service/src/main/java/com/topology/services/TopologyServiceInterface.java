package com.topology.services;

import com.topology.dto.CustomerPathResponse;
import com.topology.dto.FdhTopologyResponse;
import com.topology.dto.HeadendTopologyDto;
import com.topology.dto.InfrastructurePathResponse;
import reactor.core.publisher.Mono;

public interface TopologyServiceInterface {
    public Mono<CustomerPathResponse> traceCustomerPath(Long customerId);
    public Mono<Object> traceDevicePath(String serialNumber);
    public Mono<InfrastructurePathResponse> traceInfrastructurePath(String serialNumber);
    public Mono<HeadendTopologyDto>getHeadendTopology(Long headendId);
    public Mono<FdhTopologyResponse> getFdhTopology(Long fdhId);
}
