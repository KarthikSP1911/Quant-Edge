package com.quantedge.backend.resolver;

import com.quantedge.backend.dto.response.AuditLogPageResponse;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.service.AuditLogService;
import org.springframework.graphql.data.method.annotation.Argument;
import org.springframework.graphql.data.method.annotation.QueryMapping;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;

@Controller
public class AuditLogResolver {

    private final AuditLogService auditLogService;

    public AuditLogResolver(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    @QueryMapping
    public AuditLogPageResponse activityTimeline(
            @AuthenticationPrincipal User user,
            @Argument String action,
            @Argument String entityType,
            @Argument String startDate,
            @Argument String endDate,
            @Argument int page,
            @Argument int size) {
        return auditLogService.activityTimeline(user, action, entityType, startDate, endDate, page, size);
    }
}
