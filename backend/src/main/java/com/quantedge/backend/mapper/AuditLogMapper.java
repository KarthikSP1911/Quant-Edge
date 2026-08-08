package com.quantedge.backend.mapper;

import com.quantedge.backend.dto.response.AuditLogEntryResponse;
import com.quantedge.backend.entity.AuditLog;
import org.springframework.stereotype.Component;

@Component
public class AuditLogMapper {

    public AuditLogEntryResponse toResponse(AuditLog auditLog) {
        return new AuditLogEntryResponse(
                auditLog.getId(),
                auditLog.getAction(),
                auditLog.getEntityType(),
                auditLog.getEntityId(),
                auditLog.getDetails(),
                auditLog.getIpAddress(),
                auditLog.getCreatedAt());
    }
}
