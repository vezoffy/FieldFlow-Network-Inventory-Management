package com.training.customer_service.clients;

import com.training.customer_service.dtos.feign.AssetAssignRequest;
import com.training.customer_service.dtos.AssetResponse;
import com.training.customer_service.dtos.SplitterDto;
import com.training.customer_service.dtos.SplitterUpdateRequest;
import com.training.customer_service.exceptions.InventoryServiceException;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class InventoryServiceProxy {

    private final WebClient webClient;

    public InventoryServiceProxy(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public AssetResponse assignAssetToCustomer(String serialNumber, Long customerId) {
        try {
            AssetAssignRequest request = new AssetAssignRequest();
            request.setCustomerId(customerId);

            return webClient.patch()
                    .uri("http://inventory-service/api/inventory/assets/{serialNumber}/assign", serialNumber)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new InventoryServiceException("Failed to assign asset: " + body)))
                    )
                    .bodyToMono(AssetResponse.class)
                    .block();
        } catch (Exception e) {
            throw new InventoryServiceException("Error communicating with Inventory Service: " + e.getMessage());
        }
    }

    public AssetResponse getAssetBySerial(String serialNumber) {
        try {
            return webClient.get()
                    .uri("http://inventory-service/api/inventory/assets/{serialNumber}", serialNumber)
                    .retrieve()
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new InventoryServiceException("Asset not found: " + serialNumber)))
                    )
                    .onStatus(status -> status.is5xxServerError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new InventoryServiceException("Inventory Service Error: " + body)))
                    )
                    .bodyToMono(AssetResponse.class)
                    .block();
        } catch (Exception e) {
            throw new InventoryServiceException("Error communicating with Inventory Service: " + e.getMessage());
        }
    }

    public List<AssetResponse> getAssetsByCustomerId(Long customerId) {
        try {
            return webClient.get()
                    .uri("http://inventory-service/api/inventory/assets/customer/{customerId}", customerId)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new InventoryServiceException("Inventory Service Error: " + body)))
                    )
                    .bodyToMono(new ParameterizedTypeReference<List<AssetResponse>>() {})
                    .block();
        } catch (Exception e) {
            throw new InventoryServiceException("Error communicating with Inventory Service: " + e.getMessage());
        }
    }

    public SplitterDto getSplitterDetails(Long splitterId) {
        try {
            return webClient.get()
                    .uri("http://inventory-service/api/inventory/splitters/{id}", splitterId)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new InventoryServiceException("Failed to get splitter details: " + body)))
                    )
                    .bodyToMono(SplitterDto.class)
                    .block();
        } catch (Exception e) {
            throw new InventoryServiceException("Error communicating with Inventory Service: " + e.getMessage());
        }
    }

    public SplitterDto updateSplitterUsedPorts(Long splitterId, SplitterUpdateRequest request) {
        try {
            return webClient.patch()
                    .uri("http://inventory-service/api/inventory/splitters/{id}/used-ports", splitterId)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(status -> status.isError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> Mono.error(new InventoryServiceException("Failed to update splitter used ports: " + body)))
                    )
                    .bodyToMono(SplitterDto.class)
                    .block();
        } catch (Exception e) {
            throw new InventoryServiceException("Error communicating with Inventory Service: " + e.getMessage());
        }
    }
}