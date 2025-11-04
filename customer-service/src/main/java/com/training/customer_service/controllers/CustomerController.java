package com.training.customer_service.controllers;

import com.training.customer_service.dtos.*;
import com.training.customer_service.dtos.AssetResponse;
import com.training.customer_service.enums.CustomerStatus;
import com.training.customer_service.service.CustomerService;
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

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<List<CustomerResponse>> getAllCustomers() {
        return ResponseEntity.ok(customerService.getAllCustomers());
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<CustomerResponse> createCustomer(@RequestBody CustomerCreateRequest request) {
        CustomerResponse customer = customerService.createCustomer(request);
        return new ResponseEntity<>(customer, HttpStatus.CREATED);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT', 'TECHNICIAN')")
    public ResponseEntity<CustomerResponse> getCustomerById(@PathVariable Long id) {
        CustomerResponse customer = customerService.getCustomerById(id);
        return ResponseEntity.ok(customer);
    }

    @GetMapping("/splitter/{splitterId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<List<CustomerAssignmentDto>> getCustomersBySplitter(@PathVariable Long splitterId) {
        List<CustomerAssignmentDto> customers = customerService.getCustomersBySplitter(splitterId);
        return ResponseEntity.ok(customers);
    }

    @GetMapping("/{id}/assignment")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<CustomerAssignmentDto> getCustomerAssignment(@PathVariable Long id) {
        CustomerAssignmentDto assignment = customerService.getCustomerAssignment(id);
        return ResponseEntity.ok(assignment);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<CustomerResponse> updateCustomerProfile(@PathVariable Long id, @RequestBody CustomerCreateRequest request) {
        CustomerResponse customer = customerService.updateCustomerProfile(id, request);
        return ResponseEntity.ok(customer);
    }

    @PatchMapping("/{customerId}/assign-port")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<CustomerResponse> assignSplitterPort(@PathVariable Long customerId, @RequestBody CustomerAssignmentRequest assignment) {
        CustomerResponse customer = customerService.assignSplitterPort(customerId, assignment);
        return ResponseEntity.ok(customer);
    }

    @PostMapping("/{customerId}/assign-asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'TECHNICIAN')")
    public ResponseEntity<AssetResponse> assignAssetToCustomer(@PathVariable Long customerId, @RequestBody AssetAssignRequest request) {
        AssetResponse asset = customerService.assignAssetToCustomer(customerId, request.getAssetSerialNumber());
        return ResponseEntity.ok(asset);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'SUPPORT_AGENT')")
    public ResponseEntity<List<CustomerResponse>> searchCustomers(
            @RequestParam(required = false) String neighborhood,
            @RequestParam(required = false) CustomerStatus status,
            @RequestParam(required = false) String address,
            @RequestParam(required = false) String name) {
        List<CustomerResponse> customers = customerService.searchCustomers(neighborhood, status, address, name);
        return ResponseEntity.ok(customers);
    }
}
