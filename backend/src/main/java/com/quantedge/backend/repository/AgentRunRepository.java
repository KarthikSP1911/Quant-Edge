package com.quantedge.backend.repository;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.AgentRun;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
