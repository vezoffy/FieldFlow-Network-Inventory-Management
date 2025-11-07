package com.deploymentservice.service;

import com.deploymentservice.clients.AuthClient;
import com.deploymentservice.clients.InventoryClient;
import com.deploymentservice.clients.CustomerClient;
import com.deploymentservice.dto.AssetReclaimRequest;
import com.deploymentservice.dto.TaskCreationRequest;
import com.deploymentservice.dto.TechnicianCreationRequest;
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
public class DeploymentService implements DeploymentServiceInterface {

    private final DeploymentTaskRepository deploymentTaskRepository;

    private final TechnicianRepository technicianRepository;

    private final AuditLogService auditLogService;

    private final CustomerClient customerClient;

    private final InventoryClient inventoryClient;

    private final AuthClient authClient;

    @Autowired
    public DeploymentService(DeploymentTaskRepository deploymentTaskRepository,
                             TechnicianRepository technicianRepository,
                             AuditLogService auditLogService,
                             CustomerClient customerClient,
                             InventoryClient inventoryClient,
                             AuthClient authClient)
    {
        this.deploymentTaskRepository = deploymentTaskRepository;
        this.technicianRepository = technicianRepository;
        this.auditLogService = auditLogService;
        this.customerClient = customerClient;
        this.inventoryClient = inventoryClient;
        this.authClient = authClient;
    }

    @Transactional
    public Mono<Technician> createTechnician(TechnicianCreationRequest request, String adminUserId) {
        // 1. Register user in Auth Service
        AuthClient.AuthServiceUserRequest authRequest = new AuthClient.AuthServiceUserRequest(
                request.getUsername(), request.getPassword(), "TECHNICIAN");

        return authClient.registerUser(authRequest)
                .then(Mono.defer(() -> {
                    // 2. If Auth Service registration is successful, save Technician in Deployment Service
                    Technician technician = new Technician();
                    technician.setName(request.getName());
                    technician.setUsername(request.getUsername());
                    technician.setContact(request.getContact());
                    technician.setRegion(request.getRegion());
                    Technician savedTechnician = technicianRepository.save(technician);
                    auditLogService.logAction(adminUserId, "TECHNICIAN_CREATED", "Technician " + savedTechnician.getName() + " (" + savedTechnician.getUsername() + ") created.");
                    return Mono.just(savedTechnician);
                }))
                .onErrorResume(ServiceCommunicationException.class, e -> {
                    auditLogService.logAction(adminUserId, "TECHNICIAN_CREATION_FAILED", "Failed to create technician " + request.getUsername() + ": " + e.getMessage());
                    return Mono.error(e); // Re-throw the exception
                });
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
        if(technicianId == null)
        {
            return Flux.fromIterable(deploymentTaskRepository.findAll());
        }
        else {
            return Flux.fromIterable(deploymentTaskRepository.findByTechnicianId(technicianId));
        }
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


    public List<Technician> getAllTechniciansOrByRegion(String region) {
        if (region != null) {
            return technicianRepository.findByRegionContainingIgnoreCase(region);
        } else {
            return technicianRepository.findAll();
        }
    }
}