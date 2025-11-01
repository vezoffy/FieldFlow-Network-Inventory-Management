package com.deploymentservice.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class TaskCreationRequest {
    private Long customerId;
    private Long technicianId;
    private LocalDate scheduledDate;
}
