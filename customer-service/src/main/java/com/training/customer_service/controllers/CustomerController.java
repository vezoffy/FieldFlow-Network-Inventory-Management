package com.training.customer_service.controllers;

import com.training.customer_service.dtos.*;
import com.training.customer_service.enums.CustomerStatus;
import com.training.customer_service.service.CustomerServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/customers")
public class CustomerController {

    private final CustomerServiceInterface customerService;

    @Autowired
    public CustomerController(CustomerServiceInterface customerServiceInterface) {
        this.customerService= customerServiceInterface;
    }

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

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteCustomer(@PathVariable Long id) {
        customerService.deleteInactiveCustomer(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/assign-port")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<CustomerResponse> assignSplitterPort(@PathVariable Long id, @RequestBody CustomerAssignmentRequest assignment) {
        CustomerResponse customer = customerService.assignSplitterPort(id, assignment);
        return ResponseEntity.ok(customer);
    }

    @PatchMapping("/{id}/reassign-port")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER')")
    public ResponseEntity<CustomerResponse> reassignSplitterPort(@PathVariable Long id, @RequestBody CustomerAssignmentRequest assignment) {
        CustomerResponse customer = customerService.reassignSplitterPort(id, assignment);
        return ResponseEntity.ok(customer);
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

    @PostMapping("/{id}/assign-asset")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'TECHNICIAN')")
    public ResponseEntity<AssetResponse> assignAssetToCustomer(@PathVariable Long id, @RequestBody AssetAssignRequest request) {
        AssetResponse asset = customerService.assignAssetToCustomer(id, request.getAssetSerialNumber());
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
