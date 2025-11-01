package com.deploymentservice.repository;

import com.deploymentservice.entity.DeploymentTask;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface DeploymentTaskRepository extends JpaRepository<DeploymentTask, Long> {
    List<DeploymentTask> findByTechnicianId(Long technicianId);
}
