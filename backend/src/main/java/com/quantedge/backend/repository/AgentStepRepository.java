package com.quantedge.backend.repository;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.AgentStep;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentStepRepository extends JpaRepository<AgentStep, UUID> {

    List<AgentStep> findByAgentRunIdOrderByStepNumberAsc(UUID agentRunId);
}
