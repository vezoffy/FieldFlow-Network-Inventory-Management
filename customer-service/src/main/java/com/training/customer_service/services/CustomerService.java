package com.training.customer_service.services;

import com.training.customer_service.clients.InventoryServiceProxy;
import com.training.customer_service.dtos.*;
import com.training.customer_service.dtos.feign.AssetDetailDto;
import com.training.customer_service.dtos.feign.AssetResponse;
import com.training.customer_service.dtos.feign.SplitterDto;
import com.training.customer_service.dtos.feign.SplitterUpdateRequest;
import com.training.customer_service.entities.Customer;
import com.training.customer_service.entities.FiberDropLine;
import com.training.customer_service.enums.CustomerStatus;
import com.training.customer_service.enums.FiberStatus;
import com.training.customer_service.exceptions.CustomerNotFoundException;
import com.training.customer_service.exceptions.InvalidPortAssignmentException;
import com.training.customer_service.exceptions.InventoryServiceException;
import com.training.customer_service.repositories.CustomerRepository;
import com.training.customer_service.repositories.FiberDropLineRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class CustomerService {

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private FiberDropLineRepository fiberDropLineRepository;

    @Autowired
    private InventoryServiceProxy inventoryServiceProxy;

    @Transactional
    public AssetResponse assignAssetToCustomer(Long customerId, String assetSerialNumber) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException("Customer with ID " + customerId + " not found.");
        }
        return inventoryServiceProxy.assignAssetToCustomer(assetSerialNumber, customerId);
    }

    @Transactional
    public CustomerResponse createCustomer(CustomerCreateRequest request) {
        Customer customer = new Customer();
        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setNeighborhood(request.getNeighborhood());
        customer.setPlan(request.getPlan());
        customer.setConnectionType(request.getConnectionType());
        customer.setStatus(CustomerStatus.PENDING);

        Customer savedCustomer = customerRepository.save(customer);
        return mapToCustomerResponse(savedCustomer, Collections.emptyList());
    }

    public CustomerResponse getCustomerById(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + id + " not found."));

        List<AssetResponse> assignedAssets = inventoryServiceProxy.getAssetsByCustomerId(id);

        return mapToCustomerResponse(customer, assignedAssets);
    }

    @Transactional
    public CustomerResponse updateCustomerProfile(Long id, CustomerCreateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + id + " not found."));

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setNeighborhood(request.getNeighborhood());
        customer.setPlan(request.getPlan());
        customer.setConnectionType(request.getConnectionType());

        Customer updatedCustomer = customerRepository.save(customer);
        return mapToCustomerResponse(updatedCustomer, Collections.emptyList());
    }

    @Transactional
    public CustomerResponse deactivateCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + id + " not found."));

        // If customer was assigned to a splitter, decrement used ports
        if (customer.getSplitterId() != null) {
            try {
                SplitterDto splitter = inventoryServiceProxy.getSplitterDetails(customer.getSplitterId());
                if (splitter.getUsedPorts() > 0) {
                    SplitterUpdateRequest updateRequest = new SplitterUpdateRequest();
                    updateRequest.setUsedPorts(splitter.getUsedPorts() - 1);
                    inventoryServiceProxy.updateSplitterUsedPorts(splitter.getId(), updateRequest);
                }
            } catch (Exception e) {
                // Log the error but don't prevent customer deactivation
                System.err.println("Failed to decrement splitter used ports for splitter ID " + customer.getSplitterId() + ": " + e.getMessage());
            }
        }

        customer.setStatus(CustomerStatus.INACTIVE);
        Customer updatedCustomer = customerRepository.save(customer);
        return mapToCustomerResponse(updatedCustomer, Collections.emptyList());
    }

    @Transactional
    public CustomerResponse assignSplitterPort(Long customerId, CustomerAssignmentRequest assignment) {
        if (assignment.portNumber() == null || assignment.portNumber() <= 0) {
            throw new InvalidPortAssignmentException("Assigned port number must be a positive integer.");
        }

        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException("Customer not found with ID: " + customerId));

        AssetResponse splitterAsset = inventoryServiceProxy.getAssetBySerial(assignment.splitterSerialNumber());

        // Check if port is already in use (local check)
        boolean portInUse = customerRepository.findAll().stream()
                .anyMatch(c -> c.getSplitterId() != null && c.getSplitterId().equals(splitterAsset.getId()) &&
                              c.getAssignedPort() != null && c.getAssignedPort().equals(assignment.portNumber()) &&
                              c.getStatus() == CustomerStatus.ACTIVE);

        if (portInUse) {
            throw new InvalidPortAssignmentException("Port " + assignment.portNumber() + " on splitter " + assignment.splitterSerialNumber() + " is already in use.");
        }

        // Update splitter used ports in inventory-service
        try {
            SplitterDto splitter = inventoryServiceProxy.getSplitterDetails(splitterAsset.getId());
            if (splitter.getUsedPorts() >= splitter.getPortCapacity()) {
                throw new InvalidPortAssignmentException("Splitter " + splitter.getSerialNumber() + " is at full capacity.");
            }
            SplitterUpdateRequest updateRequest = new SplitterUpdateRequest();
            updateRequest.setUsedPorts(splitter.getUsedPorts() + 1);
            inventoryServiceProxy.updateSplitterUsedPorts(splitter.getId(), updateRequest);
        } catch (Exception e) {
            throw new InventoryServiceException("Failed to update splitter used ports in Inventory Service: " + e.getMessage());
        }

        customer.setSplitterId(splitterAsset.getId());
        customer.setSplitterSerialNumber(assignment.splitterSerialNumber());
        customer.setAssignedPort(assignment.portNumber());
        customer.setStatus(CustomerStatus.ACTIVE);
        Customer updatedCustomer = customerRepository.save(customer);

        Optional<FiberDropLine> existingLine = fiberDropLineRepository.findByCustomerId(customerId);
        FiberDropLine fiberLine = existingLine.orElse(new FiberDropLine());
        fiberLine.setCustomerId(customerId);
        fiberLine.setFromSplitterId(splitterAsset.getId());
        fiberLine.setLengthMeters(assignment.lengthMeters());
        fiberLine.setStatus(FiberStatus.ACTIVE);
        fiberDropLineRepository.save(fiberLine);

        return mapToCustomerResponse(updatedCustomer, Collections.emptyList());
    }

    public List<CustomerResponse> searchCustomers(String neighborhood, CustomerStatus status, String address) {
        List<Customer> customers;
        if (neighborhood != null && status != null) {
            customers = customerRepository.findByNeighborhoodAndStatus(neighborhood, status);
        } else if (address != null && !address.isBlank()) {
            customers = customerRepository.findByAddressContaining(address);
        } else {
            customers = customerRepository.findAll();
        }
        return customers.stream().map(c -> mapToCustomerResponse(c, Collections.emptyList())).collect(Collectors.toList());
    }

    public CustomerAssignmentDto getCustomerAssignment(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException("Customer with ID " + id + " not found."));
        return toCustomerAssignmentDto(customer);
    }

    public List<CustomerAssignmentDto> getCustomersBySplitter(Long splitterId) {
        return customerRepository.findBySplitterId(splitterId).stream()
                .map(this::toCustomerAssignmentDto)
                .collect(Collectors.toList());
    }

    private CustomerAssignmentDto toCustomerAssignmentDto(Customer customer) {
        CustomerAssignmentDto dto = new CustomerAssignmentDto();
        dto.setCustomerId(customer.getId());
        dto.setName(customer.getName());
        dto.setSplitterId(customer.getSplitterId());
        dto.setAssignedPort(customer.getAssignedPort());
        dto.setStatus(customer.getStatus().name());

        List<AssetResponse> assets = inventoryServiceProxy.getAssetsByCustomerId(customer.getId());
        List<AssetDetailDto> assetDetails = assets.stream().map(asset -> {
            AssetDetailDto detailDto = new AssetDetailDto();
            detailDto.setAssetType(asset.getAssetType());
            detailDto.setSerialNumber(asset.getSerialNumber());
            detailDto.setModel(asset.getModel());
            return detailDto;
        }).collect(Collectors.toList());
        dto.setAssignedAssets(assetDetails);

        return dto;
    }

    private CustomerResponse mapToCustomerResponse(Customer customer, List<AssetResponse> assignedAssets) {
        CustomerResponse response = new CustomerResponse();
        response.setId(customer.getId());
        response.setName(customer.getName());
        response.setAddress(customer.getAddress());
        response.setNeighborhood(customer.getNeighborhood());
        response.setPlan(customer.getPlan());
        response.setConnectionType(customer.getConnectionType());
        response.setStatus(customer.getStatus());
        response.setSplitterSerialNumber(customer.getSplitterSerialNumber());
        response.setSplitterId(customer.getSplitterId());
        response.setAssignedPort(customer.getAssignedPort());
        response.setCreatedAt(customer.getCreatedAt());
        response.setAssignedAssets(assignedAssets);
        return response;
    }
}
