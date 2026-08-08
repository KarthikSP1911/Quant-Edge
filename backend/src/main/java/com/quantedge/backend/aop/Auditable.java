package com.quantedge.backend.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a state-changing service method for automatic audit logging. {@link AuditAspect} writes
 * one {@code audit_logs} row per successful invocation - annotated methods never log manually.
 *
 * <p>{@code entityId} is a SpEL expression evaluated against the method's arguments (by name) and,
 * after the method returns, against {@code #result}. Example: {@code "#result.id()"} for a record
 * return type, or {@code "#symbol"} for a plain String argument.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Auditable {

    String action();

    String entityType();

    String entityId();
}
