package com.deploymentservice.service;

import com.deploymentservice.clients.AuthClient;
import com.deploymentservice.clients.CustomerClient;
import com.deploymentservice.clients.InventoryClient;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor; // No longer needed for the failing tests but might be used by others
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDate;
import java.util.ArrayList; // <-- Added this import
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DeploymentServiceUnitTest {

    @Mock
    private DeploymentTaskRepository deploymentTaskRepository;

    @Mock
    private TechnicianRepository technicianRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private CustomerClient customerClient;

    @Mock
    private InventoryClient inventoryClient;

    @Mock
    private AuthClient authClient;

    @InjectMocks
    private DeploymentService deploymentService;

    private Technician technician;
    private DeploymentTask scheduledTask;

    @BeforeEach
    void setUp() {
        technician = new Technician();
        technician.setId(1L);
        technician.setName("Test Tech");
        technician.setUsername("testtech");
        technician.setRegion("North");

        scheduledTask = new DeploymentTask();
        scheduledTask.setId(1L);
        scheduledTask.setCustomerId(101L);
        scheduledTask.setTechnicianId(1L);
        scheduledTask.setStatus(TaskStatus.SCHEDULED);
        scheduledTask.setScheduledDate(LocalDate.now());
    }

    @Test
    void createTechnician_Success() {
        TechnicianCreationRequest request = new TechnicianCreationRequest();
        request.setUsername("newtech");
        request.setPassword("pass");
        request.setName("New Technician");
        request.setRegion("South");

        when(authClient.registerUser(any(AuthClient.AuthServiceUserRequest.class))).thenReturn(Mono.empty());
        when(technicianRepository.save(any(Technician.class))).thenReturn(technician);

        Mono<Technician> result = deploymentService.createTechnician(request, "admin123");

        StepVerifier.create(result)
                .expectNext(technician)
                .verifyComplete();

        verify(authClient).registerUser(any(AuthClient.AuthServiceUserRequest.class));
        verify(technicianRepository).save(any(Technician.class));
        verify(auditLogService).logAction(anyString(), eq("TECHNICIAN_CREATED"), anyString());
    }

    @Test
    void createTechnician_AuthClientFailure_LogsErrorAndThrowsException() {
        TechnicianCreationRequest request = new TechnicianCreationRequest();
        request.setUsername("failtech");

        when(authClient.registerUser(any(AuthClient.AuthServiceUserRequest.class)))
                .thenReturn(Mono.error(new ServiceCommunicationException("Auth service down")));

        Mono<Technician> result = deploymentService.createTechnician(request, "admin123");

        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof ServiceCommunicationException &&
                        e.getMessage().contains("Auth service down"))
                .verify();

        verify(auditLogService).logAction(anyString(), eq("TECHNICIAN_CREATION_FAILED"), anyString());
        verify(technicianRepository, never()).save(any(Technician.class));
    }

    @Test
    void createTask_Success() {
        TaskCreationRequest request = new TaskCreationRequest();
        request.setCustomerId(101L);
        request.setTechnicianId(1L);
        request.setScheduledDate(LocalDate.now());

        when(deploymentTaskRepository.save(any(DeploymentTask.class))).thenReturn(scheduledTask);

        Mono<DeploymentTask> result = deploymentService.createTask(request, "user123");

        StepVerifier.create(result)
                .expectNext(scheduledTask)
                .verifyComplete();

        verify(deploymentTaskRepository).save(any(DeploymentTask.class));
        verify(auditLogService).logAction(anyString(), eq("TASK_CREATED"), anyString());
    }

    @Test
    void getTasksByTechnician_AllTasks() {
        List<DeploymentTask> allTasks = Arrays.asList(scheduledTask, new DeploymentTask());
        when(deploymentTaskRepository.findAll()).thenReturn(allTasks);

        Flux<DeploymentTask> result = deploymentService.getTasksByTechnician(null);

        StepVerifier.create(result)
                .expectNextCount(2)
                .verifyComplete();

        verify(deploymentTaskRepository).findAll();
        verify(deploymentTaskRepository, never()).findByTechnicianId(anyLong());
    }

    @Test
    void getTasksByTechnician_FilteredByTechId() {
        List<DeploymentTask> techTasks = Collections.singletonList(scheduledTask);
        when(deploymentTaskRepository.findByTechnicianId(1L)).thenReturn(techTasks);

        Flux<DeploymentTask> result = deploymentService.getTasksByTechnician(1L);

        StepVerifier.create(result)
                .expectNext(scheduledTask)
                .verifyComplete();

        verify(deploymentTaskRepository).findByTechnicianId(1L);
        verify(deploymentTaskRepository, never()).findAll();
    }

    // --- ⬇️ HERE IS THE FIRST FIX ⬇️ ---
    @Test
    void completeInstallation_Success() {
        String notes = "Installation complete.";
        DeploymentTask initialTask = new DeploymentTask();
        initialTask.setId(1L);
        initialTask.setCustomerId(101L);
        initialTask.setTechnicianId(1L);
        initialTask.setStatus(TaskStatus.SCHEDULED);
        initialTask.setNotes("Initial notes.");
        initialTask.setScheduledDate(LocalDate.now());

        when(deploymentTaskRepository.findById(1L)).thenReturn(Optional.of(initialTask));

        // --- FIX: Manually capture the status at the time of each call ---
        List<TaskStatus> statusAtSave = new ArrayList<>();
        when(deploymentTaskRepository.save(any(DeploymentTask.class))).thenAnswer(invocation -> {
            DeploymentTask taskToSave = invocation.getArgument(0);
            // 1. Capture the status *at this exact moment*
            statusAtSave.add(taskToSave.getStatus());
            // 2. Return the same object to simulate JPA's behavior
            return taskToSave;
        });
        // --- END FIX ---

        when(customerClient.updateCustomerStatus(anyLong(), eq("ACTIVE"))).thenReturn(Mono.empty());

        Mono<DeploymentTask> result = deploymentService.completeInstallation(1L, notes, "tech123");

        StepVerifier.create(result)
                .expectNextMatches(task -> task.getStatus() == TaskStatus.COMPLETED &&
                        task.getNotes().contains("Installation started: Installation complete.") &&
                        task.getNotes().contains("Installation completed successfully."))
                .verifyComplete();

        // Verify the number of save calls
        verify(deploymentTaskRepository, times(2)).save(any(DeploymentTask.class));

        // --- FIX: Assert against our manually captured list ---
        assertEquals(TaskStatus.IN_PROGRESS, statusAtSave.get(0));
        assertEquals(TaskStatus.COMPLETED, statusAtSave.get(1));
        // --- END FIX ---

        verify(auditLogService).logAction(anyString(), eq("INSTALLATION_STARTED"), anyString());
        verify(customerClient).updateCustomerStatus(101L, "ACTIVE");
        verify(auditLogService).logAction(anyString(), eq("TASK_COMPLETION"), anyString());
    }

    @Test
    void completeInstallation_TaskNotFound_ThrowsException() {
        when(deploymentTaskRepository.findById(anyLong())).thenReturn(Optional.empty());

        Mono<DeploymentTask> result = deploymentService.completeInstallation(1L, "notes", "user");

        StepVerifier.create(result)
                .expectError(DeploymentTaskNotFoundException.class)
                .verify();
    }

    // --- ⬇️ HERE IS THE SECOND FIX ⬇️ ---
    @Test
    void completeInstallation_CustomerClientFails_LogsAndThrowsException() {
        String notes = "Installation complete.";
        DeploymentTask inProgressTask = new DeploymentTask();
        inProgressTask.setId(1L);
        inProgressTask.setCustomerId(101L);
        inProgressTask.setTechnicianId(1L);
        inProgressTask.setStatus(TaskStatus.SCHEDULED);
        inProgressTask.setScheduledDate(LocalDate.now());

        when(deploymentTaskRepository.findById(1L)).thenReturn(Optional.of(inProgressTask));

        // --- FIX: Manually capture the status at the time of each call ---
        List<TaskStatus> statusAtSave = new ArrayList<>();
        when(deploymentTaskRepository.save(any(DeploymentTask.class))).thenAnswer(invocation -> {
            DeploymentTask taskToSave = invocation.getArgument(0);
            // 1. Capture the status *at this exact moment*
            statusAtSave.add(taskToSave.getStatus());
            // 2. Return the same object to simulate JPA's behavior
            return taskToSave;
        });
        // --- END FIX ---

        when(customerClient.updateCustomerStatus(anyLong(), anyString()))
                .thenReturn(Mono.error(new ServiceCommunicationException("Customer service error")));

        Mono<DeploymentTask> result = deploymentService.completeInstallation(1L, notes, "tech123");

        StepVerifier.create(result)
                .expectErrorMatches(e -> e instanceof ServiceCommunicationException &&
                        e.getMessage().contains("Installation failed due to customer service communication error"))
                .verify();

        // Verify the number of save calls
        verify(deploymentTaskRepository, times(2)).save(any(DeploymentTask.class)); // Once for IN_PROGRESS, once for FAILED

        // --- FIX: Assert against our manually captured list ---
        assertEquals(TaskStatus.IN_PROGRESS, statusAtSave.get(0));
        assertEquals(TaskStatus.FAILED, statusAtSave.get(1));
        // --- END FIX ---

        verify(auditLogService).logAction(anyString(), eq("INSTALLATION_STARTED"), anyString());
        verify(auditLogService).logAction(anyString(), eq("INSTALLATION_FAILED"), anyString());
    }

    @Test
    void deactivateCustomerWorkflow_Success() {
        Long customerId = 101L;
        String reason = "Moved";
        String userId = "admin123";

        when(customerClient.updateCustomerStatus(customerId, "INACTIVE")).thenReturn(Mono.empty());
        when(inventoryClient.reclaimAssetsByCustomer(eq(customerId), any(AssetReclaimRequest.class))).thenReturn(Mono.empty());

        Mono<Void> result = deploymentService.deactivateCustomerWorkflow(customerId, reason, userId);

        StepVerifier.create(result)
                .verifyComplete();

        verify(customerClient).updateCustomerStatus(customerId, "INACTIVE");
        verify(auditLogService).logAction(eq(userId), eq("CUSTOMER_DEACTIVATION"), eq("Customer 101 deactivated. Reason: Moved"));
        verify(inventoryClient).reclaimAssetsByCustomer(eq(customerId), any(AssetReclaimRequest.class));
    }

    @Test
    void deactivateCustomerWorkflow_CustomerClientFails_LogsError() {
        Long customerId = 101L;
        String reason = "Moved";
        String userId = "admin123";

        when(customerClient.updateCustomerStatus(customerId, "INACTIVE"))
                .thenReturn(Mono.error(new ServiceCommunicationException("Customer service error")));

        Mono<Void> result = deploymentService.deactivateCustomerWorkflow(customerId, reason, userId);

        StepVerifier.create(result)
                .expectError(ServiceCommunicationException.class)
                .verify();

        verify(auditLogService).logAction(eq(userId), eq("CUSTOMER_DEACTIVATION_FAILED"), anyString());
        verify(inventoryClient, never()).reclaimAssetsByCustomer(anyLong(), any(AssetReclaimRequest.class));
    }

    @Test
    void getAllTechniciansOrByRegion_NoRegion() {
        List<Technician> technicians = Arrays.asList(technician, new Technician());
        when(technicianRepository.findAll()).thenReturn(technicians);

        List<Technician> result = deploymentService.getAllTechniciansOrByRegion(null);

        assertEquals(2, result.size());
        verify(technicianRepository).findAll();
        verify(technicianRepository, never()).findByRegionContainingIgnoreCase(anyString());
    }

    @Test
    void getAllTechniciansOrByRegion_WithRegion() {
        List<Technician> technicians = Collections.singletonList(technician);
        when(technicianRepository.findByRegionContainingIgnoreCase("North")).thenReturn(technicians);

        List<Technician> result = deploymentService.getAllTechniciansOrByRegion("North");

        assertEquals(1, result.size());
        verify(technicianRepository).findByRegionContainingIgnoreCase("North");
        verify(technicianRepository, never()).findAll();
    }
}