package com.training.customer_service.service;

import com.training.customer_service.dtos.*;
import com.training.customer_service.entities.FiberDropLine;
import com.training.customer_service.enums.CustomerStatus;

import java.util.List;

public interface CustomerServiceInterface {
    void updateCustomerStatus(Long id, String status);
    void deleteInactiveCustomer(Long id);
    CustomerResponse reassignSplitterPort(Long customerId, CustomerAssignmentRequest assignment);
    CustomerResponse assignSplitterPort(Long customerId, CustomerAssignmentRequest assignment);
    List<CustomerResponse> getAllCustomers();
    List<FiberDropLine> getFiberDropLinesBySplitter(Long splitterId);
    List<FiberDropLineResponse> getAllFiberDropLines();
    AssetResponse assignAssetToCustomer(Long customerId, String assetSerialNumber);
    CustomerResponse createCustomer(CustomerCreateRequest request);
    CustomerResponse getCustomerById(Long id);
    CustomerResponse updateCustomerProfile(Long id, CustomerCreateRequest request);
    List<CustomerResponse> searchCustomers(String neighborhood, CustomerStatus status, String address, String name);
    CustomerAssignmentDto getCustomerAssignment(Long id);
    List<CustomerAssignmentDto> getCustomersBySplitter(Long splitterId);
}
