package com.quantedge.backend.repository;

import java.time.Instant;
import java.util.UUID;

import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Query(
            """
            select a from AuditLog a
            where a.user = :user
            and (:action is null or a.action = :action)
            and (:entityType is null or a.entityType = :entityType)
            and (:from is null or a.createdAt >= :from)
            and (:to is null or a.createdAt <= :to)
            order by a.createdAt desc
            """)
    Page<AuditLog> search(
            @Param("user") User user,
            @Param("action") String action,
            @Param("entityType") String entityType,
            @Param("from") Instant from,
            @Param("to") Instant to,
            Pageable pageable);
}
