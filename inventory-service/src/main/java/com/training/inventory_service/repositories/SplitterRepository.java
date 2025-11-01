package com.training.inventory_service.repositories;

import com.training.inventory_service.entities.Splitter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface SplitterRepository extends JpaRepository<Splitter, Long> {
    List<Splitter> findByFdhId(Long fdhId);
}
