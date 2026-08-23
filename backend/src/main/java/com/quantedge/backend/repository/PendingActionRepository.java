package com.quantedge.backend.repository;

import java.util.Optional;
import java.util.UUID;

import com.quantedge.backend.entity.PendingAction;
import com.quantedge.backend.enums.PendingActionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingActionRepository extends JpaRepository<PendingAction, UUID> {

    Optional<PendingAction> findFirstByUserIdAndStatusOrderByCreatedAtDesc(UUID userId, PendingActionStatus status);
}
