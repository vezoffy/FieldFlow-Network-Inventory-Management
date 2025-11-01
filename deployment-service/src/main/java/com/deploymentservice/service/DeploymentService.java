package com.deploymentservice.service;

import com.deploymentservice.clients.CustomerClient;
import com.deploymentservice.clients.InventoryClient;
import com.deploymentservice.dto.AssetReclaimRequest;
import com.deploymentservice.dto.TaskCreationRequest;
import com.deploymentservice.dto.TaskUpdateRequest;
import com.deploymentservice.entity.DeploymentTask;
import com.deploymentservice.entity.Technician;
import com.deploymentservice.enums.TaskStatus;
import com.deploymentservice.exceptions.DeploymentTaskNotFoundException;
import com.deploymentservice.exceptions.ServiceCommunicationException;
import com.deploymentservice.repository.DeploymentTaskRepository;
import com.deploymentservice.repository.TechnicianRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
public class DeploymentService {

    @Autowired
    private DeploymentTaskRepository deploymentTaskRepository;

    @Autowired
    private TechnicianRepository technicianRepository;

    @Autowired
    private AuditLogService auditLogService;

    @Autowired
    private CustomerClient customerClient;

    @Autowired
    private InventoryClient inventoryClient;

    @Transactional
    public Mono<Technician> createTechnician(Technician technician) {
        return Mono.fromCallable(() -> technicianRepository.save(technician));
    }

    @Transactional
    public Mono<DeploymentTask> createTask(TaskCreationRequest request, String userId) {
        return Mono.fromCallable(() -> {
            DeploymentTask task = new DeploymentTask();
            task.setCustomerId(request.getCustomerId());
            task.setTechnicianId(request.getTechnicianId());
            task.setScheduledDate(request.getScheduledDate());
            task.setStatus(TaskStatus.SCHEDULED);
            DeploymentTask savedTask = deploymentTaskRepository.save(task);
            auditLogService.logAction(userId, "TASK_CREATED", "Deployment task " + savedTask.getId() + " created for customer " + savedTask.getCustomerId());
            return savedTask;
        });
    }

    public Flux<DeploymentTask> getTasksByTechnician(Long technicianId) {
        return Flux.fromIterable(deploymentTaskRepository.findByTechnicianId(technicianId));
    }

    @Transactional
    public Mono<DeploymentTask> completeInstallation(Long taskId, String notes, String userId) {
        return Mono.fromCallable(() -> deploymentTaskRepository.findById(taskId)
                .orElseThrow(() -> new DeploymentTaskNotFoundException("Deployment task not found with ID: " + taskId)))
                .flatMap(task -> {
                    task.setStatus(TaskStatus.IN_PROGRESS);
                    task.setNotes(task.getNotes() != null ? task.getNotes() + "\nInstallation started: " + notes : "Installation started: " + notes);
                    deploymentTaskRepository.save(task);
                    auditLogService.logAction(userId, "INSTALLATION_STARTED", "Installation started for task " + taskId + ". Customer: " + task.getCustomerId());

                    return customerClient.updateCustomerStatus(task.getCustomerId(), "ACTIVE")
                            .then(Mono.fromCallable(() -> {
                                task.setStatus(TaskStatus.COMPLETED);
                                task.setNotes(task.getNotes() + "\nInstallation completed successfully.");
                                DeploymentTask completedTask = deploymentTaskRepository.save(task);
                                auditLogService.logAction(userId, "TASK_COMPLETION", "Installation task " + taskId + " completed for customer " + task.getCustomerId());
                                return completedTask;
                            }))
                            .onErrorResume(ServiceCommunicationException.class, e -> {
                                task.setStatus(TaskStatus.FAILED);
                                task.setNotes(task.getNotes() + "\nInstallation failed: " + e.getMessage());
                                deploymentTaskRepository.save(task);
                                auditLogService.logAction(userId, "INSTALLATION_FAILED", "Installation task " + taskId + " failed for customer " + task.getCustomerId() + ": " + e.getMessage());
                                return Mono.error(new ServiceCommunicationException("Installation failed due to customer service communication error: " + e.getMessage()));
                            });
                });
    }

    @Transactional
    public Mono<Void> deactivateCustomerWorkflow(Long customerId, String reason, String userId) {
        return customerClient.updateCustomerStatus(customerId, "INACTIVE")
                .doOnSuccess(aVoid -> auditLogService.logAction(userId, "CUSTOMER_DEACTIVATION", "Customer " + customerId + " deactivated. Reason: " + reason))
                .then(Mono.defer(() -> {
                    AssetReclaimRequest reclaimRequest = new AssetReclaimRequest();
                    reclaimRequest.setStatus("Available");
                    reclaimRequest.setAssignedToCustomer(null);
                    return inventoryClient.reclaimAssetsByCustomer(customerId, reclaimRequest);
                }))
                .doOnError(e -> auditLogService.logAction(userId, "CUSTOMER_DEACTIVATION_FAILED", "Customer " + customerId + " deactivation failed: " + e.getMessage()));
    }
}
