package com.deploymentservice.clients;

import com.deploymentservice.exceptions.ServiceCommunicationException;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
public class CustomerClient {

    private final WebClient.Builder webClientBuilder;

    @Autowired
    public CustomerClient(WebClient.Builder webClientBuilder) {
        this.webClientBuilder = webClientBuilder;
    }

    public Mono<Void> updateCustomerStatus(Long customerId, String status) {
        CustomerStatusUpdateRequest request = new CustomerStatusUpdateRequest(status);

        return webClientBuilder.build().patch()
                .uri("lb://CUSTOMER-SERVICE/api/customers/{id}/status", customerId)
                .bodyValue(request)
                .retrieve()
                .onStatus(httpStatus -> httpStatus.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(errorBody -> Mono.error(new ServiceCommunicationException("Failed to update customer status: " + errorBody)))
                )
                .bodyToMono(Void.class);
    }

    // Inner DTO for the request body
    @Data
    @AllArgsConstructor
    private static class CustomerStatusUpdateRequest {
        private String status;
    }
}
