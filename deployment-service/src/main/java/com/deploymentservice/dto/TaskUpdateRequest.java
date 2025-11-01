package com.deploymentservice.dto;

import com.deploymentservice.enums.TaskStatus;
import lombok.Data;

@Data
public class TaskUpdateRequest {
    private TaskStatus status;
    private String notes;
}
