package com.topology.clients;

import com.topology.dto.CustomerAssignmentDto;
import com.topology.exceptions.TopologyServiceException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
public class CustomerClient {

    private final WebClient webClient;

    @Autowired
    public CustomerClient(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public Mono<CustomerAssignmentDto> getCustomerAssignment(Long id) {
        return webClient.get()
                .uri("http://customer-service/api/customers/{id}/assignment", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Customer Service Error: " + body)))
                )
                .bodyToMono(CustomerAssignmentDto.class);
    }

    public Mono<List<CustomerAssignmentDto>> getCustomersBySplitter(Long id) {
        return webClient.get()
                .uri("http://customer-service/api/customers/splitter/{id}", id)
                .retrieve()
                .onStatus(status -> status.isError(), response ->
                        response.bodyToMono(String.class)
                                .flatMap(body -> Mono.error(new TopologyServiceException("Customer Service Error: " + body)))
                )
                .bodyToMono(new ParameterizedTypeReference<List<CustomerAssignmentDto>>() {});
    }
}
