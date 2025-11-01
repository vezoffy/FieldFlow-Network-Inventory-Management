package com.topology.clients;

import com.topology.dto.*;
import com.topology.exceptions.TopologyServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class InventoryClient {

    private final WebClient webClient;

    @Autowired
    public InventoryClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<HeadendDto> getHeadendDetails(Long id) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/headends/{id}", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(HeadendDto.class);
    }

    public Mono<HeadendTopologyDto> getHeadendTopology(Long id) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/headends/{id}/topology", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(HeadendTopologyDto.class);
    }

    public Mono<CoreSwitchDto> getCoreSwitchDetails(Long id) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/core-switches/{id}", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(CoreSwitchDto.class);
    }

    public Mono<FdhDto> getFdhDetails(Long id) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/fdhs/{id}", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(FdhDto.class);
    }

    public Mono<SplitterDto> getSplitterDetails(Long id) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/splitters/{id}", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(SplitterDto.class);
    }

    public Mono<List<SplitterDto>> getSplittersByFdh(Long id) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/fdhs/{id}/splitters", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(new ParameterizedTypeReference<List<SplitterDto>>() {});
    }

    public Mono<AssetAssignmentDetailsDto> getAssetAssignmentDetails(String serialNumber) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/assets/assignment/{serialNumber}", serialNumber)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(AssetAssignmentDetailsDto.class);
    }

    // New method to get full asset details by serial number
    public Mono<AssetResponse> getAssetBySerial(String serialNumber) {
        return webClient.get()
                .uri("http://inventory-service/api/inventory/assets/{serialNumber}", serialNumber)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Inventory Service Error: " + body)))
                )
                .bodyToMono(AssetResponse.class);
    }
}
