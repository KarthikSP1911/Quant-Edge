package com.quantedge.backend.repository;

import java.util.List;
import java.util.UUID;

import com.quantedge.backend.entity.AgentRun;
import com.quantedge.backend.enums.AgentRunStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRunRepository extends JpaRepository<AgentRun, UUID> {

    List<AgentRun> findByUserIdOrderByCreatedAtDesc(UUID userId);

    /** Most recent completed runs for this user+company, used to seed a new run's long-term memory. */
    List<AgentRun> findByUserIdAndCompanyIdAndStatusOrderByCreatedAtDesc(
            UUID userId, UUID companyId, AgentRunStatus status, Pageable pageable);
}
