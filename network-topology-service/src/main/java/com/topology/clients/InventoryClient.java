package com.topology.clients;

import com.topology.dto.*;
import com.topology.exceptions.TopologyServiceException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientException;
import reactor.core.publisher.Mono;

import java.util.List;
import java.util.function.Predicate;

@Component
public class InventoryClient {

    private static final Logger logger = LoggerFactory.getLogger(InventoryClient.class);
    private final WebClient webClient;

    // --- Sonar: Use constants for repeated strings ---
    private static final String INVENTORY_SERVICE_ERROR_MSG = "Inventory Service Error";
    private static final String COMMUNICATION_ERROR_MSG = "Error communicating with Inventory Service";

    // --- Sonar: Context constants for logging ---
    private static final String CTX_GET_HEADEND = "getHeadendDetails";
    private static final String CTX_GET_HEADEND_TOPO = "getHeadendTopology";
    private static final String CTX_GET_CORE_SWITCH = "getCoreSwitchDetails";
    private static final String CTX_GET_FDH = "getFdhDetails";
    private static final String CTX_GET_SPLITTER = "getSplitterDetails";
    private static final String CTX_GET_SPLITTERS_BY_FDH = "getSplittersByFdh";
    private static final String CTX_GET_ASSET_ASSIGNMENT = "getAssetAssignmentDetails";
    private static final String CTX_GET_ASSET_BY_SERIAL = "getAssetBySerial";


    @Autowired
    public InventoryClient(WebClient.Builder webClientBuilder,
                           // --- Sonar: Inject base URL, don't hardcode it ---
                           @Value("${inventory.service.base-url:http://inventory-service}") String inventoryServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(inventoryServiceUrl) // Set base URL once
                .build();
    }

    // --- Sonar: Reusable predicate for error status ---
    private static Predicate<HttpStatusCode> isErrorStatus() {
        return status -> status.is4xxClientError() || status.is5xxServerError();
    }

    /**
     * --- Sonar: Reusable error handler for WebClient responses ---
     * Logs the error and wraps it in a custom exception.
     * @param response The ClientResponse
     * @param context  A string describing the operation that failed (for logging)
     * @return A Mono<Throwable>
     */
    private Mono<Throwable> handleErrorResponse(ClientResponse response, String context) {
        return response.bodyToMono(String.class)
                // Use defaultIfEmpty in case the error body is empty
                .defaultIfEmpty("No error details provided")
                .flatMap(body -> {
                    String errorMsg = String.format("%s. Context: %s. Status: %s. Body: %s",
                            INVENTORY_SERVICE_ERROR_MSG, context, response.statusCode().value(), body);
                    // --- Sonar: Log the error ---
                    logger.error(errorMsg);
                    return Mono.error(new TopologyServiceException(errorMsg));
                });
    }

    /**
     * --- Sonar: Reusable handler for communication errors (e.g., connection refused) ---
     */
    private Throwable handleCommunicationError(WebClientException e, String context) {
        String errorMsg = String.format("%s. Context: %s. Message: %s",
                COMMUNICATION_ERROR_MSG, context, e.getMessage());
        logger.error(errorMsg, e); // Log the full exception
        // --- Sonar: Wrap and rethrow with cause ---
        return new TopologyServiceException(errorMsg, e);
    }

    public Mono<HeadendDto> getHeadendDetails(Long id) {
        return webClient.get()
                // --- Sonar: Use relative URLs ---
                .uri("/api/inventory/headends/{id}", id)
                .retrieve()
                // --- Sonar: Use reusable error handler ---
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_HEADEND))
                .bodyToMono(HeadendDto.class)
                // --- Sonar: Handle communication errors ---
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_HEADEND));
    }

    public Mono<HeadendTopologyDto> getHeadendTopology(Long id) {
        return webClient.get()
                .uri("/api/inventory/headends/{id}/topology", id)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_HEADEND_TOPO))
                .bodyToMono(HeadendTopologyDto.class)
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_HEADEND_TOPO));
    }

    public Mono<CoreSwitchDto> getCoreSwitchDetails(Long id) {
        return webClient.get()
                .uri("/api/inventory/core-switches/{id}", id)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_CORE_SWITCH))
                .bodyToMono(CoreSwitchDto.class)
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_CORE_SWITCH));
    }

    public Mono<FdhDto> getFdhDetails(Long id) {
        return webClient.get()
                .uri("/api/inventory/fdhs/{id}", id)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_FDH))
                .bodyToMono(FdhDto.class)
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_FDH));
    }

    public Mono<SplitterDto> getSplitterDetails(Long id) {
        return webClient.get()
                .uri("/api/inventory/splitters/{id}", id)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_SPLITTER))
                .bodyToMono(SplitterDto.class)
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_SPLITTER));
    }

    public Mono<List<SplitterDto>> getSplittersByFdh(Long id) {
        return webClient.get()
                .uri("/api/inventory/fdhs/{id}/splitters", id)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_SPLITTERS_BY_FDH))
                .bodyToMono(new ParameterizedTypeReference<List<SplitterDto>>() {})
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_SPLITTERS_BY_FDH));
    }

    public Mono<AssetAssignmentDetailsDto> getAssetAssignmentDetails(String serialNumber) {
        return webClient.get()
                .uri("/api/inventory/assets/assignment/{serialNumber}", serialNumber)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_ASSET_ASSIGNMENT))
                .bodyToMono(AssetAssignmentDetailsDto.class)
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_ASSET_ASSIGNMENT));
    }

    public Mono<AssetResponse> getAssetBySerial(String serialNumber) {
        return webClient.get()
                .uri("/api/inventory/assets/{serialNumber}", serialNumber)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_ASSET_BY_SERIAL))
                .bodyToMono(AssetResponse.class)
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_ASSET_BY_SERIAL));
    }
}