package com.deploymentservice.clients;

import com.deploymentservice.dto.AssetReclaimRequest;
import com.deploymentservice.exceptions.ServiceCommunicationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class InventoryClient {

    private final WebClient.Builder webClientBuilder;

    @Autowired
    public InventoryClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Void> reclaimAssetsByCustomer(Long customerId, AssetReclaimRequest request) {
        return webClientBuilder.build().patch()
                .uri("lb://INVENTORY-SERVICE/api/inventory/assets/unassign/customer/{customerId}", customerId)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new ServiceCommunicationException("Failed to reclaim assets: " + errorBody)))
                )
                .bodyToMono(Void.class);
    }
}
