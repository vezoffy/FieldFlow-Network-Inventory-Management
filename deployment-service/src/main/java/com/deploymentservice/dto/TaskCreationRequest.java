package com.deploymentservice.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class TaskCreationRequest {
    private Long customerId;
    private Long technicianId;
    private LocalDate scheduledDate;
}
