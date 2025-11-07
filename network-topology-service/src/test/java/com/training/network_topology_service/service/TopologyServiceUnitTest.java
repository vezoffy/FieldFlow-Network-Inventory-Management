package com.training.network_topology_service.service;

import com.topology.clients.CustomerClient;
import com.topology.clients.InventoryClient;
import com.topology.dto.*;
import com.topology.exceptions.CustomerInactiveException;
import com.topology.services.TopologyService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TopologyServiceUnitTest {

    @Mock
    private CustomerClient customerClient;

    @Mock
    private InventoryClient inventoryClient;

    @InjectMocks
    private TopologyService topologyService;

    @Test
    void traceCustomerPath_Success() {
        // Arrange
        Long customerId = 1L;

        // Mock data from Customer Service
        CustomerAssignmentDto customerDto = new CustomerAssignmentDto();
        customerDto.setCustomerId(customerId);
        customerDto.setName("Test Customer");
        customerDto.setSplitterId(10L);
        customerDto.setStatus("ACTIVE");

        // Mock data from Inventory Service
        SplitterDto splitterDto = new SplitterDto();
        splitterDto.setId(10L);
        splitterDto.setFdhId(20L);
        splitterDto.setSerialNumber("SPLITTER-SN");

        FdhDto fdhDto = new FdhDto();
        fdhDto.setId(20L);
        fdhDto.setCoreSwitchId(30L);
        fdhDto.setName("FDH-01");

        CoreSwitchDto coreSwitchDto = new CoreSwitchDto();
        coreSwitchDto.setId(30L);
        coreSwitchDto.setHeadendId(40L);
        coreSwitchDto.setName("CS-01");

        HeadendDto headendDto = new HeadendDto();
        headendDto.setId(40L);
        headendDto.setName("Main Headend");

        // Mock the client calls
        when(customerClient.getCustomerAssignment(customerId)).thenReturn(Mono.just(customerDto));
        when(inventoryClient.getSplitterDetails(10L)).thenReturn(Mono.just(splitterDto));
        when(inventoryClient.getFdhDetails(20L)).thenReturn(Mono.just(fdhDto));
        when(inventoryClient.getCoreSwitchDetails(30L)).thenReturn(Mono.just(coreSwitchDto));
        when(inventoryClient.getHeadendDetails(40L)).thenReturn(Mono.just(headendDto));

        // Act
        Mono<CustomerPathResponse> resultMono = topologyService.traceCustomerPath(customerId);

        // Assert
        StepVerifier.create(resultMono)
                .assertNext(response -> {
                    assertEquals(customerId, response.getCustomerId());
                    assertEquals("Test Customer", response.getCustomerName());
                    HierarchicalNetworkNode headendNode = response.getPath(); // Corrected method name
                    assertEquals("Main Headend", headendNode.getIdentifier());
                    assertEquals("CS-01", headendNode.getChild().getIdentifier());
                    assertEquals("FDH-01", headendNode.getChild().getChild().getIdentifier());
                    assertEquals("Splitter-10", headendNode.getChild().getChild().getChild().getIdentifier());
                    assertEquals("Test Customer", headendNode.getChild().getChild().getChild().getChild().getIdentifier());
                })
                .verifyComplete();
    }

    @Test
    void traceCustomerPath_Fails_When_CustomerIsInactive() {
        // Arrange
        Long customerId = 2L;
        CustomerAssignmentDto customerDto = new CustomerAssignmentDto();
        customerDto.setCustomerId(customerId);
        customerDto.setStatus("INACTIVE"); // Set status to INACTIVE

        when(customerClient.getCustomerAssignment(customerId)).thenReturn(Mono.just(customerDto));

        // Act
        Mono<CustomerPathResponse> resultMono = topologyService.traceCustomerPath(customerId);

        // Assert
        StepVerifier.create(resultMono)
                .expectError(CustomerInactiveException.class)
                .verify();
    }
}
