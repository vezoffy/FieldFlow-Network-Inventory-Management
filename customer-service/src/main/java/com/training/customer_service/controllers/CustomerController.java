package com.training.customer_service.controllers;

import com.training.customer_service.dtos.*;
import com.training.customer_service.dtos.feign.AssetResponse;
import com.training.customer_service.enums.CustomerStatus;
import com.training.customer_service.services.CustomerService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @PostMapping("/{customerId}/assign-asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'TECHNICIAN')")
    public ResponseEntity<AssetResponse> assignAssetToCustomer(
            @PathVariable Long customerId,
            @Valid @RequestBody AssetAssignmentRequest request) {
        AssetResponse response = customerService.assignAssetToCustomer(customerId, request.getAssetSerialNumber());
        return ResponseEntity.ok(response);
    }

    // ... existing endpoints ...

    @GetMapping("/{id}/assignment")
    public ResponseEntity<CustomerAssignmentDto> getCustomerAssignment(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomerAssignment(id));
    }

    @GetMapping("/splitter/{id}")
    public ResponseEntity<List<CustomerAssignmentDto>> getCustomersBySplitter(@PathVariable Long id) {
        return ResponseEntity.ok(customerService.getCustomersBySplitter(id));
    }
    
    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse response = customerService.createCustomer(request);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<CustomerResponse> updateCustomerProfile(@PathVariable Long id, @Valid @RequestBody CustomerCreateRequest request) {
        CustomerResponse updatedCustomer = customerService.updateCustomerProfile(id, request);
        return ResponseEntity.ok(updatedCustomer);
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT')")
    public ResponseEntity<CustomerResponse> deactivateCustomer(@PathVariable Long id) {
        CustomerResponse updatedCustomer = customerService.deactivateCustomer(id);
        return ResponseEntity.ok(updatedCustomer);
    }

    @PatchMapping("/{id}/assign-port")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<CustomerResponse> assignNetworkPort(@PathVariable Long id, @Valid @RequestBody CustomerAssignmentRequest request) {
        CustomerResponse updatedCustomer = customerService.assignSplitterPort(id, request);
        return ResponseEntity.ok(updatedCustomer);
    }

    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String address) {
        List<CustomerResponse> customers = customerService.searchCustomers(neighborhood, status, address);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/test/customer")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<String> testEndpoint() {
        return ResponseEntity.ok("Customer service is up and running!");
    }
}
