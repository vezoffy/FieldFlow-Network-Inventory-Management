package com.topology.clients;

import com.topology.dto.CustomerAssignmentDto;
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
public class CustomerClient {

    private static final Logger logger = LoggerFactory.getLogger(CustomerClient.class);
    private final WebClient webClient;

    // --- Sonar: Use constants for repeated strings ---
    private static final String CUSTOMER_SERVICE_ERROR_MSG = "Customer Service Error";
    private static final String COMMUNICATION_ERROR_MSG = "Error communicating with Customer Service";

    // --- Sonar: Context constants for logging ---
    private static final String CTX_GET_CUSTOMER_ASSIGNMENT = "getCustomerAssignment";
    private static final String CTX_GET_CUSTOMERS_BY_SPLITTER = "getCustomersBySplitter";

    @Autowired
    public CustomerClient(WebClient.Builder webClientBuilder,
                          // --- Sonar: Inject base URL, don't hardcode it ---
                          @Value("${customer.service.base-url:http://customer-service}") String customerServiceUrl) {
        this.webClient = webClientBuilder
                .baseUrl(customerServiceUrl) // Set base URL once
                .build();
    }

    // --- Sonar: Reusable predicate for error status ---
    private static Predicate<HttpStatusCode> isErrorStatus() {
        return status -> status.is4xxClientError() || status.is5xxServerError();
    }

    /**
     * --- Sonar: Reusable error handler for WebClient responses ---
     */
    private Mono<Throwable> handleErrorResponse(ClientResponse response, String context) {
        return response.bodyToMono(String.class)
                .defaultIfEmpty("No error details provided")
                .flatMap(body -> {
                    String errorMsg = String.format("%s. Context: %s. Status: %s. Body: %s",
                            CUSTOMER_SERVICE_ERROR_MSG, context, response.statusCode().value(), body);
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
        return new TopologyServiceException(errorMsg, e);
    }

    public Mono<CustomerAssignmentDto> getCustomerAssignment(Long id) {
        return webClient.get()
                // --- Sonar: Use relative URLs ---
                .uri("/api/customers/{id}/assignment", id)
                .retrieve()
                // --- Sonar: Use reusable error handler ---
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_CUSTOMER_ASSIGNMENT))
                .bodyToMono(CustomerAssignmentDto.class)
                // --- Sonar: Handle communication errors ---
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_CUSTOMER_ASSIGNMENT));
    }

    public Mono<List<CustomerAssignmentDto>> getCustomersBySplitter(Long id) {
        return webClient.get()
                .uri("/api/customers/splitter/{id}", id)
                .retrieve()
                .onStatus(isErrorStatus(), resp -> handleErrorResponse(resp, CTX_GET_CUSTOMERS_BY_SPLITTER))
                .bodyToMono(new ParameterizedTypeReference<List<CustomerAssignmentDto>>() {})
                .onErrorMap(WebClientException.class, e -> handleCommunicationError(e, CTX_GET_CUSTOMERS_BY_SPLITTER));
    }
}