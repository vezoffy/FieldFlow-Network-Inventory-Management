package com.training.customer_service.service;

import com.training.customer_service.clients.InventoryServiceProxy;
import com.training.customer_service.dtos.*;
import com.training.customer_service.entities.Customer;
import com.training.customer_service.entities.FiberDropLine;
import com.training.customer_service.enums.CustomerStatus;
import com.training.customer_service.enums.FiberStatus;
import com.training.customer_service.exceptions.CustomerActionException;
import com.training.customer_service.exceptions.CustomerNotFoundException;
import com.training.customer_service.exceptions.InvalidPortAssignmentException;
import com.training.customer_service.exceptions.InventoryServiceException;
import com.training.customer_service.repositories.CustomerRepository;
import com.training.customer_service.repositories.CustomerSpecification;
import com.training.customer_service.repositories.FiberDropLineRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
public class CustomerService implements CustomerServiceInterface {

    private static final Logger logger = LoggerFactory.getLogger(CustomerService.class);

    // --- Sonar: Constants for repeated string literals ---
    private static final String CUSTOMER_NOT_FOUND_MSG = "Customer with ID %d not found.";
    private static final String CUSTOMER_NOT_FOUND_GENERIC = "Customer not found with ID: %d";
    private static final String CANNOT_ACTIVATE_NO_SPLITTER_MSG = "Customer cannot be activated without being assigned to a splitter port.";
    private static final String CANNOT_ACTIVATE_NO_FIBER_MSG = "Customer cannot be activated without a corresponding Fiber Drop Line entry.";
    private static final String FAILED_TO_DECREMENT_PORTS_MSG = "Failed to decrement splitter used ports for splitter ID {}: {}";
    private static final String FAILED_TO_RELEASE_OLD_PORT_MSG = "Failed to release old splitter port: %s";
    private static final String SPLITTER_FULL_MSG = "Splitter %s is at full capacity.";
    private static final String FAILED_TO_UPDATE_SPLITTER_MSG = "Failed to update splitter used ports: %s";
    private static final String CUSTOMER_ALREADY_ASSIGNED_MSG = "Customer is already assigned to a port. Use the re-assign endpoint to move them.";
    private static final String FIBER_LINE_NOT_FOUND_MSG = "FiberDropLine not found for customer: %d";
    private static final String CANNOT_DELETE_ACTIVE_CUSTOMER_MSG = "Cannot delete customer. Status must be INACTIVE before deletion.";


    private final CustomerRepository customerRepository;
    private final FiberDropLineRepository fiberDropLineRepository;
    private final InventoryServiceProxy inventoryServiceProxy;

    @Autowired
    public CustomerService(CustomerRepository customerRepository, FiberDropLineRepository fiberDropLineRepository, InventoryServiceProxy inventoryServiceProxy) {
        this.customerRepository = customerRepository;
        this.fiberDropLineRepository = fiberDropLineRepository;
        this.inventoryServiceProxy = inventoryServiceProxy;
    }

    @Transactional
    public void updateCustomerStatus(Long id, String status) {
        logger.info("Attempting to update status for customer ID {} to {}", id, status);
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_MSG, id)));

        CustomerStatus newStatus = CustomerStatus.valueOf(status.toUpperCase());
        CustomerStatus oldStatus = customer.getStatus();

        if (newStatus == CustomerStatus.ACTIVE) {
            if (customer.getSplitterId() == null) {
                throw new CustomerActionException(CANNOT_ACTIVATE_NO_SPLITTER_MSG);
            }
            if (fiberDropLineRepository.findByCustomerId(id).isEmpty()) {
                throw new CustomerActionException(CANNOT_ACTIVATE_NO_FIBER_MSG);
            }
        }

        if (oldStatus == CustomerStatus.ACTIVE && newStatus == CustomerStatus.INACTIVE) {
            logger.info("Deactivation workflow triggered for customer ID {}.", id);

            if (customer.getSplitterId() != null) {
                try {
                    logger.info("Decrementing used ports for splitter ID {}.", customer.getSplitterId());
                    SplitterDto splitter = inventoryServiceProxy.getSplitterDetails(customer.getSplitterId());
                    if (splitter.getUsedPorts() > 0) {
                        SplitterUpdateRequest updateRequest = new SplitterUpdateRequest();
                        updateRequest.setUsedPorts(splitter.getUsedPorts() - 1);
                        inventoryServiceProxy.updateSplitterUsedPorts(splitter.getId(), updateRequest);
                        logger.info("Successfully decremented used ports for splitter ID {}.", customer.getSplitterId());
                    }
                } catch (InventoryServiceException e) { // --- Sonar: Catch specific exception
                    // --- Sonar: Log full exception ---
                    logger.error(FAILED_TO_DECREMENT_PORTS_MSG, customer.getSplitterId(), e.getMessage(), e);
                    // Business decision: Continue deactivation even if port decrement fails
                }
            }

            logger.info("Nullifying port assignment for customer ID {}.", id);
            customer.setSplitterId(null);
            customer.setSplitterSerialNumber(null);
            customer.setAssignedPort(null);

            fiberDropLineRepository.findByCustomerId(id).ifPresent(line -> {
                logger.info("Setting FiberDropLine status to DISCONNECTED for customer ID {}.", id);
                line.setStatus(FiberStatus.DISCONNECTED);
                fiberDropLineRepository.save(line);
            });
        }

        customer.setStatus(newStatus);
        logger.info("Saving final state for customer ID {}. Status: {}, Splitter ID: {}.", id, customer.getStatus(), customer.getSplitterId());
        customerRepository.save(customer);
    }

    @Transactional
    public void deleteInactiveCustomer(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_MSG, id)));

        if (customer.getStatus() != CustomerStatus.INACTIVE) {
            throw new CustomerActionException(CANNOT_DELETE_ACTIVE_CUSTOMER_MSG);
        }

        fiberDropLineRepository.findByCustomerId(id).ifPresent(line -> {
            logger.info("Deleting FiberDropLine for customer ID {}.", id);
            fiberDropLineRepository.delete(line);
        });

        logger.info("Deleting INACTIVE customer with ID {}.", id);
        customerRepository.delete(customer);
    }

    @Transactional
    public CustomerResponse reassignSplitterPort(Long customerId, CustomerAssignmentRequest assignment) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_GENERIC, customerId)));

        Long oldSplitterId = customer.getSplitterId();
        AssetResponse newSplitterAsset = inventoryServiceProxy.getAssetBySerial(assignment.splitterSerialNumber());

        // 1. Decrement old splitter port count
        if (oldSplitterId != null) {
            try {
                SplitterDto oldSplitter = inventoryServiceProxy.getSplitterDetails(oldSplitterId);
                if (oldSplitter.getUsedPorts() > 0) {
                    SplitterUpdateRequest updateRequest = new SplitterUpdateRequest();
                    updateRequest.setUsedPorts(oldSplitter.getUsedPorts() - 1);
                    inventoryServiceProxy.updateSplitterUsedPorts(oldSplitterId, updateRequest);
                }
            } catch (InventoryServiceException e) { // --- Sonar: Catch specific exception
                // --- Sonar: Log and re-throw to roll back transaction ---
                throw new CustomerActionException(String.format(FAILED_TO_RELEASE_OLD_PORT_MSG, e.getMessage()));
            }
        }

        // 2. Increment new splitter port count
        try {
            SplitterDto newSplitter = inventoryServiceProxy.getSplitterDetails(newSplitterAsset.getId());
            if (newSplitter.getUsedPorts() >= newSplitter.getPortCapacity()) {
                throw new InvalidPortAssignmentException(String.format(SPLITTER_FULL_MSG, newSplitter.getSerialNumber()));
            }
            SplitterUpdateRequest updateRequest = new SplitterUpdateRequest();
            updateRequest.setUsedPorts(newSplitter.getUsedPorts() + 1);
            inventoryServiceProxy.updateSplitterUsedPorts(newSplitter.getId(), updateRequest);
        } catch (InventoryServiceException e) { // --- Sonar: Catch specific exception
            // --- Sonar: Re-throw with original cause ---
            throw new InventoryServiceException(String.format(FAILED_TO_UPDATE_SPLITTER_MSG, e.getMessage()));
        }

        // 3. Update customer's assignment
        customer.setSplitterId(newSplitterAsset.getId());
        customer.setSplitterSerialNumber(assignment.splitterSerialNumber());
        customer.setAssignedPort(assignment.portNumber());
        Customer updatedCustomer = customerRepository.save(customer);

        // 4. Update FiberDropLine
        FiberDropLine fiberLine = fiberDropLineRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new InventoryServiceException(String.format(FIBER_LINE_NOT_FOUND_MSG, customerId)));
        fiberLine.setFromSplitterId(newSplitterAsset.getId());
        fiberDropLineRepository.save(fiberLine);

        return mapToCustomerResponse(updatedCustomer, inventoryServiceProxy.getAssetsByCustomerId(customerId));
    }

    @Transactional
    public CustomerResponse assignSplitterPort(Long customerId, CustomerAssignmentRequest assignment) {
        logger.info("Assigning new port for customer ID: {}", customerId);
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_GENERIC, customerId)));

        if (customer.getSplitterId() != null) {
            throw new CustomerActionException(CUSTOMER_ALREADY_ASSIGNED_MSG);
        }

        AssetResponse splitterAsset = inventoryServiceProxy.getAssetBySerial(assignment.splitterSerialNumber());

        try {
            SplitterDto splitter = inventoryServiceProxy.getSplitterDetails(splitterAsset.getId());
            if (splitter.getUsedPorts() >= splitter.getPortCapacity()) {
                throw new InvalidPortAssignmentException(String.format(SPLITTER_FULL_MSG, splitter.getSerialNumber()));
            }
            SplitterUpdateRequest updateRequest = new SplitterUpdateRequest();
            updateRequest.setUsedPorts(splitter.getUsedPorts() + 1);
            inventoryServiceProxy.updateSplitterUsedPorts(splitter.getId(), updateRequest);
        } catch (InventoryServiceException e) { // --- Sonar: Catch specific exception
            // --- Sonar: Re-throw with original cause ---
            throw new InventoryServiceException(String.format(FAILED_TO_UPDATE_SPLITTER_MSG, e.getMessage()));
        }

        customer.setSplitterId(splitterAsset.getId());
        customer.setSplitterSerialNumber(assignment.splitterSerialNumber());
        customer.setAssignedPort(assignment.portNumber());
        Customer updatedCustomer = customerRepository.save(customer);

        FiberDropLine fiberLine = new FiberDropLine();
        fiberLine.setCustomerId(customerId);
        fiberLine.setFromSplitterId(splitterAsset.getId());
        fiberLine.setLengthMeters(assignment.lengthMeters());
        fiberLine.setStatus(FiberStatus.ACTIVE);
        fiberDropLineRepository.save(fiberLine);

        return mapToCustomerResponse(updatedCustomer, inventoryServiceProxy.getAssetsByCustomerId(customerId));
    }

    public List<CustomerResponse> getAllCustomers() {
        // --- Sonar: Fixed N+1 performance bug. ---
        // Do not fetch assets for *all* customers in a list view.
        return customerRepository.findAll().stream()
                .map(customer -> mapToCustomerResponse(customer, Collections.emptyList()))
                .toList();
    }

    public List<FiberDropLine> getFiberDropLinesBySplitter(Long splitterId) {
        return fiberDropLineRepository.findByFromSplitterId(splitterId);
    }

    public List<FiberDropLineResponse> getAllFiberDropLines() {
        return fiberDropLineRepository.findAll().stream()
                .map(this::toFiberDropLineResponse)
                .toList();
    }

    @Transactional
    public AssetResponse assignAssetToCustomer(Long customerId, String assetSerialNumber) {
        if (!customerRepository.existsById(customerId)) {
            throw new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_MSG, customerId));
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
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_MSG, id)));
        List<AssetResponse> assignedAssets = inventoryServiceProxy.getAssetsByCustomerId(id);
        return mapToCustomerResponse(customer, assignedAssets);
    }

    @Transactional
    public CustomerResponse updateCustomerProfile(Long id, CustomerCreateRequest request) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_MSG, id)));

        customer.setName(request.getName());
        customer.setAddress(request.getAddress());
        customer.setNeighborhood(request.getNeighborhood());
        customer.setPlan(request.getPlan());
        customer.setConnectionType(request.getConnectionType());

        Customer updatedCustomer = customerRepository.save(customer);
        // Do not fetch assets here, only profile is updated
        return mapToCustomerResponse(updatedCustomer, Collections.emptyList());
    }

    public List<CustomerResponse> searchCustomers(String neighborhood, CustomerStatus status, String address, String name) {
        Specification<Customer> spec = CustomerSpecification.isAnything();

        if (name != null && !name.isBlank()) {
            spec = spec.and(CustomerSpecification.hasName(name));
        }
        if (address != null && !address.isBlank()) {
            spec = spec.and(CustomerSpecification.hasAddress(address));
        }
        if (neighborhood != null && !neighborhood.isBlank()) {
            spec = spec.and(CustomerSpecification.hasNeighborhood(neighborhood));
        }
        if (status != null) {
            spec = spec.and(CustomerSpecification.hasStatus(status));
        }

        List<Customer> customers = customerRepository.findAll(spec);
        return customers.stream()
                .map(c -> mapToCustomerResponse(c, Collections.emptyList()))
                .toList();
    }

    public CustomerAssignmentDto getCustomerAssignment(Long id) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new CustomerNotFoundException(String.format(CUSTOMER_NOT_FOUND_MSG, id)));
        // --- Sonar: Fixed N+1 by fetching assets here, not in the mapper ---
        List<AssetResponse> assets = inventoryServiceProxy.getAssetsByCustomerId(id);
        return toCustomerAssignmentDto(customer, assets);
    }

    public List<CustomerAssignmentDto> getCustomersBySplitter(Long splitterId) {
        List<Customer> customers = customerRepository.findBySplitterIdAndStatus(splitterId, CustomerStatus.ACTIVE);
        // --- Sonar: This N+1 call is now explicit. ---
        // Fixing it would require a new proxy method (e.g., getAssetsByCustomerIds)
        return customers.stream()
                .map(customer -> {
                    List<AssetResponse> assets = inventoryServiceProxy.getAssetsByCustomerId(customer.getId());
                    return toCustomerAssignmentDto(customer, assets);
                })
                .toList();
    }

    private FiberDropLineResponse toFiberDropLineResponse(FiberDropLine line) {
        FiberDropLineResponse dto = new FiberDropLineResponse();
        dto.setId(line.getId());
        dto.setCustomerId(line.getCustomerId());
        dto.setFromSplitterId(line.getFromSplitterId());
        if (line.getLengthMeters() != null) {
            dto.setLengthMeters(line.getLengthMeters().doubleValue());
        }
        dto.setStatus(line.getStatus());
        return dto;
    }

    // --- Sonar: Extracted asset mapping to its own method ---
    private AssetDetailDto toAssetDetailDto(AssetResponse asset) {
        AssetDetailDto detailDto = new AssetDetailDto();
        detailDto.setAssetType(asset.getAssetType());
        detailDto.setSerialNumber(asset.getSerialNumber());
        detailDto.setModel(asset.getModel());
        return detailDto;
    }

    // --- Sonar: Mapper signature changed to remove proxy call (fix N+1) ---
    private CustomerAssignmentDto toCustomerAssignmentDto(Customer customer, List<AssetResponse> assets) {
        CustomerAssignmentDto dto = new CustomerAssignmentDto();
        dto.setCustomerId(customer.getId());
        dto.setName(customer.getName());
        dto.setSplitterId(customer.getSplitterId());
        if (customer.getAssignedPort() != null) {
            dto.setAssignedPort(customer.getAssignedPort());
        }
        dto.setStatus(customer.getStatus().name());

        List<AssetDetailDto> assetDetails = assets.stream()
                .map(this::toAssetDetailDto) // --- Sonar: Use method reference ---
                .toList();
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