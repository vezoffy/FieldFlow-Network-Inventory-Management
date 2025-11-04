package com.training.customer_service.controllers;

import com.training.customer_service.service.CustomerService;
import lombok.Data;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customers")
public class DeploymentIntegrationController {

    @Autowired
    private CustomerService customerService;

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN', 'SUPPORT_AGENT', 'TECHNICIAN')") // Secured for internal system calls
    public ResponseEntity<Void> updateCustomerStatus(@PathVariable Long id, @RequestBody CustomerStatusUpdateRequest request) {
        customerService.updateCustomerStatus(id, request.getStatus());
        return ResponseEntity.ok().build();
    }

    @Data
    private static class CustomerStatusUpdateRequest {
        private String status;
    }
}
