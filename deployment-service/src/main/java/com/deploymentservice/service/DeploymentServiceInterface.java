package com.deploymentservice.service;

import com.deploymentservice.dto.TaskCreationRequest;
import com.deploymentservice.dto.TechnicianCreationRequest;
import com.deploymentservice.entity.DeploymentTask;
import com.deploymentservice.entity.Technician;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface DeploymentServiceInterface {
    Mono<Technician> createTechnician(TechnicianCreationRequest request, String adminUserId);
    Mono<DeploymentTask> createTask(TaskCreationRequest request, String userId);
    Flux<DeploymentTask> getTasksByTechnician(Long technicianId);
    Mono<DeploymentTask> completeInstallation(Long taskId, String notes, String userId);
    Mono<Void> deactivateCustomerWorkflow(Long customerId, String reason, String userId);
    List<Technician> getAllTechniciansOrByRegion(String region);
}
