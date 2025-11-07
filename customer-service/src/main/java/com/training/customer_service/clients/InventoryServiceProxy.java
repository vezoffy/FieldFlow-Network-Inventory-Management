package com.training.customer_service.clients;

import com.training.customer_service.dtos.feign.AssetAssignRequest;
import com.training.customer_service.dtos.AssetResponse;
import com.training.customer_service.dtos.SplitterDto;
import com.training.customer_service.dtos.SplitterUpdateRequest;
import com.training.customer_service.exceptions.InventoryServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Service
public class InventoryServiceProxy {

    private static final Logger logger = LoggerFactory.getLogger(InventoryServiceProxy.class);

    // --- Sonar: Constants for repeated strings ---
    private static final String COMMUNICATION_ERROR_MSG = "Error communicating with Inventory Service";
    private static final String FAILED_TO_ASSIGN_ASSET_MSG = "Failed to assign asset";
    private static final String ASSET_NOT_FOUND_MSG = "Asset not found: %s";
    private static final String FAILED_TO_GET_ASSETS_MSG = "Failed to get assets for customer";
    private static final String FAILED_TO_GET_SPLITTER_MSG = "Failed to get splitter details";
    private static final String FAILED_TO_UPDATE_SPLITTER_MSG = "Failed to update splitter used ports";

    private final WebClient webClient;

    // --- Sonar: Inject base URL, don't hardcode it ---
    public InventoryServiceProxy(WebClient.Builder webClientBuilder,
                                 @Value("${inventory.service.base-url:http://inventory-service}") String inventoryServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(inventoryServiceUrl) // Set base URL once
                .build();
    }

    private static Predicate<HttpStatusCode> isErrorStatus() {
        return status -> status.is4xxClientError() || status.is5xxServerError();
    }

    private Mono<Throwable> handleErrorResponse(ClientResponse response, String context) {
        return response.bodyToMono(String.class)
                // Use defaultIfEmpty in case the error body is empty
                .defaultIfEmpty("No error details provided")
                .flatMap(body -> {
                    String errorMsg = String.format("%s. Status: %s, Body: %s",
                            context, response.statusCode().value(), body);
                    logger.error(errorMsg);
                    return Mono.error(new InventoryServiceException(errorMsg));
                });
    }

    public AssetResponse assignAssetToCustomer(String serialNumber, Long customerId) {
        try {
            AssetAssignRequest request = new AssetAssignRequest();
            request.setCustomerId(customerId);

            return webClient.patch()
                    // --- Sonar: Use relative URLs ---
                    .uri("/api/inventory/assets/{serialNumber}/assign", serialNumber)
                    .bodyValue(request)
                    .retrieve()
                    // --- Sonar: Use reusable error handler ---
                    .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, FAILED_TO_ASSIGN_ASSET_MSG))
                    .bodyToMono(AssetResponse.class)
                    .block();
        } catch (WebClientException e) {
            // --- Sonar: Catch specific exceptions and log the full error ---

            throw new InventoryServiceException(COMMUNICATION_ERROR_MSG + ": " + e.getMessage());
        }
    }

    public AssetResponse getAssetBySerial(String serialNumber) {
        try {
            return webClient.get()
                    .uri("/api/inventory/assets/by-serial/{serialNumber}", serialNumber)
                    .retrieve()
                    // --- Sonar: Specific 404 handling ---
                    .onStatus(status -> status.is4xxClientError(), response ->
                            response.bodyToMono(String.class)
                                    .flatMap(body -> {
                                        String errorMsg = String.format(ASSET_NOT_FOUND_MSG, serialNumber);
                                        logger.warn(errorMsg);
                                        return Mono.error(new InventoryServiceException(errorMsg));
                                    })
                    )
                    .onStatus(status -> status.is5xxServerError(), resp -> handleErrorResponse(resp, "Inventory Service Error"))
                    .bodyToMono(AssetResponse.class)
                    .block();
        } catch (WebClientException e) {
            throw new InventoryServiceException(COMMUNICATION_ERROR_MSG + ": " + e.getMessage());
        }
    }

    public List<AssetResponse> getAssetsByCustomerId(Long customerId) {
        try {
            return webClient.get()
                    .uri("/api/inventory/assets/customer/{customerId}", customerId)
                    .retrieve()
                    .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, FAILED_TO_GET_ASSETS_MSG))
                    .bodyToMono(new ParameterizedTypeReference<List<AssetResponse>>() {})
                    .block();
        } catch (WebClientException e) {
            throw new InventoryServiceException(COMMUNICATION_ERROR_MSG + ": " + e.getMessage());
        }
    }

    public SplitterDto getSplitterDetails(Long splitterId) {
        try {
            return webClient.get()
                    .uri("/api/inventory/splitters/{id}", splitterId)
                    .retrieve()
                    .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, FAILED_TO_GET_SPLITTER_MSG))
                    .bodyToMono(SplitterDto.class)
                    .block();
        } catch (WebClientException e) {
            throw new InventoryServiceException(COMMUNICATION_ERROR_MSG + ": " + e.getMessage());
        }
    }

    public SplitterDto updateSplitterUsedPorts(Long splitterId, SplitterUpdateRequest request) {
        try {
            return webClient.patch()
                    .uri("/api/inventory/splitters/{id}/used-ports", splitterId)
                    .bodyValue(request)
                    .retrieve()
                    .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, FAILED_TO_UPDATE_SPLITTER_MSG))
                    .bodyToMono(SplitterDto.class)
                    .block();
        } catch (WebClientException e) {
            throw new InventoryServiceException(COMMUNICATION_ERROR_MSG + ": " + e.getMessage());
        }
    }
}