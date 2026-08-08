package com.quantedge.backend.aop;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.quantedge.backend.entity.AuditLog;
import com.quantedge.backend.entity.User;
import com.quantedge.backend.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Writes one {@code audit_logs} row per successful {@link Auditable}-annotated method call. Runs
 * after the method returns so {@code entityId} SpEL expressions can reference {@code #result};
 * a thrown exception skips logging entirely (nothing to audit for a failed action).
 */
@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class AuditAspect {

    private static final ParameterNameDiscoverer PARAMETER_NAMES = new DefaultParameterNameDiscoverer();
    private static final ExpressionParser SPEL = new SpelExpressionParser();

    // Boot 4's autoconfigured Jackson stack is tools.jackson (Jackson 3), so classic
    // com.fasterxml ObjectMapper has no bean to inject here - build a plain one instead.
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    private final AuditLogRepository auditLogRepository;

    @Around("@annotation(auditable)")
    public Object audit(ProceedingJoinPoint joinPoint, Auditable auditable) throws Throwable {
        Object result = joinPoint.proceed();

        try {
            Method method = ((MethodSignature) joinPoint.getSignature()).getMethod();
            Object[] args = joinPoint.getArgs();
            StandardEvaluationContext context = new StandardEvaluationContext();
            String[] paramNames = PARAMETER_NAMES.getParameterNames(method);
            Map<String, Object> details = new LinkedHashMap<>();
            if (paramNames != null) {
                for (int i = 0; i < paramNames.length; i++) {
                    context.setVariable(paramNames[i], args[i]);
                    if (!(args[i] instanceof User)) {
                        details.put(paramNames[i], args[i]);
                    }
                }
            }
            context.setVariable("result", result);

            Expression entityIdExpression = SPEL.parseExpression(auditable.entityId());
            Object entityId = entityIdExpression.getValue(context);

            AuditLog auditLog = AuditLog.builder()
                    .user(currentUser())
                    .action(auditable.action())
                    .entityType(auditable.entityType())
                    .entityId(entityId == null ? null : entityId.toString())
                    .details(objectMapper.writeValueAsString(details))
                    .ipAddress(currentIpAddress())
                    .build();

            auditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("Failed to write audit log for {}", auditable.action(), e);
        }

        return result;
    }

    private User currentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }

    private String currentIpAddress() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
            return attributes.getRequest().getRemoteAddr();
        }
        return null;
    }
}
