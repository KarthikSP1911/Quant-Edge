package com.quantedge.backend.dto.response;

import java.util.List;

public record AuditLogPageResponse(
        List<AuditLogEntryResponse> content, long totalElements, int totalPages, int page, int size) {}
