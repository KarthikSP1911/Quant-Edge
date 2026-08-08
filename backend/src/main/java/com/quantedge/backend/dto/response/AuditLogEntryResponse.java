package com.quantedge.backend.dto.response;

import java.time.Instant;
import java.util.UUID;

public record AuditLogEntryResponse(
        UUID id,
        String action,
        String entityType,
        String entityId,
        String details,
        String ipAddress,
        Instant createdAt) {}
