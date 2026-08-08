package com.quantedge.backend.service;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import com.quantedge.backend.dto.response.AuditLogPageResponse;
import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.mapper.AuditLogMapper;
import com.quantedge.backend.repository.AuditLogRepository;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    private AuditLogService auditLogService;

    private final User user = User.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        auditLogService = new AuditLogService(auditLogRepository, new AuditLogMapper());
    }

    @Test
    void mapsFilterArgumentsAndReturnsPagedResults() {
        AuditLog entry = AuditLog.builder()
                .id(UUID.randomUUID())
                .action("WATCHLIST_ADD")
                .entityType("WATCHLIST")
                .entityId("AAPL")
                .createdAt(Instant.parse("2026-08-06T00:00:00Z"))
                .build();
        Page<AuditLog> page = new PageImpl<>(List.of(entry), PageRequest.of(0, 20), 1);
        when(auditLogRepository.search(
                        eq(user),
                        eq("WATCHLIST_ADD"),
                        eq("WATCHLIST"),
                        eq(Instant.parse("2026-08-01T00:00:00Z")),
                        isNull(),
                        eq(PageRequest.of(0, 20))))
                .thenReturn(page);

        AuditLogPageResponse result = auditLogService.activityTimeline(
                user, "WATCHLIST_ADD", "WATCHLIST", "2026-08-01T00:00:00Z", null, 0, 20);

        assertThat(result.content()).hasSize(1);
        assertThat(result.content().get(0).action()).isEqualTo("WATCHLIST_ADD");
        assertThat(result.totalElements()).isEqualTo(1);
    }
}
