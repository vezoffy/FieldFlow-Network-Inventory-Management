package com.deploymentservice.dto;

import com.deploymentservice.enums.TaskStatus;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TaskUpdateRequest {
    private TaskStatus status;
    private String notes;
}
