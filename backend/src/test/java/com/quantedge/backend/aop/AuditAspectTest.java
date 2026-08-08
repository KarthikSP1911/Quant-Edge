package com.quantedge.backend.aop;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.repository.AuditLogRepository;
import java.lang.reflect.Method;
import java.util.UUID;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.reflect.MethodSignature;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
class AuditAspectTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private ProceedingJoinPoint joinPoint;

    @Mock
    private MethodSignature methodSignature;

    private AuditAspect auditAspect;

    private final User user = User.builder().id(UUID.randomUUID()).build();

    @BeforeEach
    void setUp() {
        auditAspect = new AuditAspect(auditLogRepository);
        SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(user, null));
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Auditable(action = "WATCHLIST_ADD", entityType = "WATCHLIST", entityId = "#symbol")
    void addToWatchlist(String symbol) {}

    @Auditable(action = "PLACE_ORDER", entityType = "ORDER", entityId = "#result")
    String placeOrder(String symbol) {
        return "order-id-123";
    }

    @Test
    void writesAuditLogWithResolvedEntityIdAndAuthenticatedUser() throws Throwable {
        Method method = getClass().getDeclaredMethod("addToWatchlist", String.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"AAPL"});
        when(joinPoint.proceed()).thenReturn(null);

        auditAspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        AuditLog saved = captor.getValue();

        assertThat(saved.getAction()).isEqualTo("WATCHLIST_ADD");
        assertThat(saved.getEntityType()).isEqualTo("WATCHLIST");
        assertThat(saved.getEntityId()).isEqualTo("AAPL");
        assertThat(saved.getUser()).isEqualTo(user);
        assertThat(saved.getDetails()).contains("AAPL");
    }

    @Test
    void resolvesEntityIdFromMethodResult() throws Throwable {
        Method method = getClass().getDeclaredMethod("placeOrder", String.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"AAPL"});
        when(joinPoint.proceed()).thenReturn("order-id-123");

        auditAspect.audit(joinPoint, auditable);

        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        verify(auditLogRepository).save(captor.capture());
        assertThat(captor.getValue().getEntityId()).isEqualTo("order-id-123");
    }

    @Test
    void returnsUnderlyingMethodResultUnchanged() throws Throwable {
        Method method = getClass().getDeclaredMethod("placeOrder", String.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        when(joinPoint.getSignature()).thenReturn(methodSignature);
        when(methodSignature.getMethod()).thenReturn(method);
        when(joinPoint.getArgs()).thenReturn(new Object[] {"AAPL"});
        when(joinPoint.proceed()).thenReturn("order-id-123");

        Object result = auditAspect.audit(joinPoint, auditable);

        assertThat(result).isEqualTo("order-id-123");
    }

    @Test
    void doesNotSwallowExceptionsFromTheUnderlyingMethod() throws Throwable {
        Method method = getClass().getDeclaredMethod("addToWatchlist", String.class);
        Auditable auditable = method.getAnnotation(Auditable.class);

        when(joinPoint.proceed()).thenThrow(new IllegalStateException("boom"));

        assertThatThrownBy(() -> auditAspect.audit(joinPoint, auditable)).isInstanceOf(IllegalStateException.class);
        verifyNoInteractions(auditLogRepository);
    }
}
