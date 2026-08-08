package com.quantedge.backend.service;

import com.quantedge.backend.dto.response.AuditLogPageResponse;
import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.mapper.AuditLogMapper;
import com.quantedge.backend.repository.AuditLogRepository;
import java.time.Instant;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuditLogService {

    private final AuditLogRepository auditLogRepository;
    private final AuditLogMapper auditLogMapper;

    public AuditLogPageResponse activityTimeline(
            User user, String action, String entityType, String startDate, String endDate, int page, int size) {
        Instant from = startDate == null ? null : Instant.parse(startDate);
        Instant to = endDate == null ? null : Instant.parse(endDate);

        Page<AuditLog> result =
                auditLogRepository.search(user, action, entityType, from, to, PageRequest.of(page, size));

        return new AuditLogPageResponse(
                result.getContent().stream().map(auditLogMapper::toResponse).toList(),
                result.getTotalElements(),
                result.getTotalPages(),
                page,
                size);
    }
}
