package com.training.customer_service.service;

import com.training.customer_service.clients.InventoryServiceProxy;
import com.training.customer_service.dtos.AssetResponse;
import com.training.customer_service.dtos.CustomerAssignmentRequest;
import com.training.customer_service.dtos.CustomerResponse;
import com.training.customer_service.dtos.SplitterDto;
import com.training.customer_service.dtos.SplitterUpdateRequest;
import com.training.customer_service.entities.Customer;
import com.training.customer_service.entities.FiberDropLine;
import com.training.customer_service.enums.CustomerStatus;
import com.training.customer_service.exceptions.CustomerActionException;
import com.training.customer_service.repositories.CustomerRepository;
import com.training.customer_service.repositories.FiberDropLineRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CustomerServiceUnitTest {

    @Mock
    private CustomerRepository customerRepository;

    @Mock
    private FiberDropLineRepository fiberDropLineRepository;

    @Mock
    private InventoryServiceProxy inventoryServiceProxy;

    @InjectMocks
    private CustomerService customerService;

    private Customer customer;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(1L);
        customer.setStatus(CustomerStatus.PENDING);
    }

    @Test
    void updateCustomerStatus_ToActive_Fails_When_NoSplitterAssigned() {
        // Arrange
        customer.setSplitterId(null); // No splitter assigned
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Act & Assert
        CustomerActionException exception = assertThrows(CustomerActionException.class, () -> {
            customerService.updateCustomerStatus(1L, "ACTIVE");
        });

        // Verify the error message from the constant
        assertEquals("Customer cannot be activated without being assigned to a splitter port.", exception.getMessage());
    }

    @Test
    void updateCustomerStatus_ToActive_Fails_When_NoFiberDropLineExists() {
        // Arrange
        customer.setSplitterId(100L); // Splitter is assigned
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(fiberDropLineRepository.findByCustomerId(1L)).thenReturn(Optional.empty()); // No fiber line

        // Act & Assert
        CustomerActionException exception = assertThrows(CustomerActionException.class, () -> {
            customerService.updateCustomerStatus(1L, "ACTIVE");
        });

        // Verify the error message from the constant
        assertEquals("Customer cannot be activated without a corresponding Fiber Drop Line entry.", exception.getMessage());
    }

    @Test
    void updateCustomerStatus_ToActive_Succeeds_When_PrerequisitesMet() {
        // Arrange
        customer.setSplitterId(100L);
        FiberDropLine fiberLine = new FiberDropLine();

        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(fiberDropLineRepository.findByCustomerId(1L)).thenReturn(Optional.of(fiberLine));

        // Act
        customerService.updateCustomerStatus(1L, "ACTIVE");

        // Assert
        assertEquals(CustomerStatus.ACTIVE, customer.getStatus());
        verify(customerRepository, times(1)).save(customer);
    }

    @Test
    void assignSplitterPort_Fails_When_CustomerAlreadyAssigned() {
        // Arrange
        customer.setSplitterId(100L); // Customer is already assigned
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));

        // Correctly instantiate the record with a BigDecimal
        CustomerAssignmentRequest request = new CustomerAssignmentRequest("some-serial", 1, new BigDecimal("10.0"));

        // Act & Assert
        CustomerActionException exception = assertThrows(CustomerActionException.class, () -> {
            customerService.assignSplitterPort(1L, request);
        });

        assertEquals("Customer is already assigned to a port. Use the re-assign endpoint to move them.", exception.getMessage());
    }

    @Test
    void reassignSplitterPort_Success() {
        // Arrange
        customer.setSplitterId(100L); // Old splitter
        when(customerRepository.findById(1L)).thenReturn(Optional.of(customer));
        when(customerRepository.save(any(Customer.class))).thenReturn(customer);

        // Mock inventory calls
        SplitterDto oldSplitter = new SplitterDto();
        oldSplitter.setUsedPorts(5);
        when(inventoryServiceProxy.getSplitterDetails(100L)).thenReturn(oldSplitter);

        AssetResponse newSplitterAsset = new AssetResponse();
        newSplitterAsset.setId(200L);
        newSplitterAsset.setSerialNumber("NEW-SPLITTER");
        when(inventoryServiceProxy.getAssetBySerial(anyString())).thenReturn(newSplitterAsset);

        SplitterDto newSplitter = new SplitterDto();
        newSplitter.setId(200L); // *** THIS IS THE FIX ***
        newSplitter.setUsedPorts(2);
        newSplitter.setPortCapacity(8);
        when(inventoryServiceProxy.getSplitterDetails(200L)).thenReturn(newSplitter);

        when(fiberDropLineRepository.findByCustomerId(1L)).thenReturn(Optional.of(new FiberDropLine()));
        when(inventoryServiceProxy.getAssetsByCustomerId(1L)).thenReturn(Collections.emptyList());

        // Correctly instantiate the record with a BigDecimal
        CustomerAssignmentRequest request = new CustomerAssignmentRequest("NEW-SPLITTER", 3, new BigDecimal("50.0"));

        // Act
        CustomerResponse response = customerService.reassignSplitterPort(1L, request);

        // Assert
        // Verify port decrement on old splitter
        verify(inventoryServiceProxy, times(1)).updateSplitterUsedPorts(eq(100L), any(SplitterUpdateRequest.class));
        // Verify port increment on new splitter
        verify(inventoryServiceProxy, times(1)).updateSplitterUsedPorts(eq(200L), any(SplitterUpdateRequest.class));
        // Verify customer is updated
        assertEquals(200L, customer.getSplitterId());
        verify(customerRepository, times(1)).save(customer);
    }
}
