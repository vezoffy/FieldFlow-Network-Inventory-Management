package com.training.customer_service.controllers;

import com.training.customer_service.dtos.FiberDropLineResponse;
import com.training.customer_service.entities.FiberDropLine;
import com.training.customer_service.service.CustomerService;
import com.training.customer_service.service.CustomerServiceInterface;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/customers/fiber-drop-lines")
public class FiberDropLineController {

    private final CustomerServiceInterface customerService;

    @Autowired
    public FiberDropLineController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'TECHNICIAN')")
    public ResponseEntity<List<FiberDropLineResponse>> getAllFiberDropLines() {
        return ResponseEntity.ok(customerService.getAllFiberDropLines());
    }

    @GetMapping("/splitter/{splitterId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'PLANNER', 'TECHNICIAN')")
    public ResponseEntity<List<FiberDropLine>> getFiberDropLinesBySplitter(@PathVariable Long splitterId) {
        return ResponseEntity.ok(customerService.getFiberDropLinesBySplitter(splitterId));
    }
}
