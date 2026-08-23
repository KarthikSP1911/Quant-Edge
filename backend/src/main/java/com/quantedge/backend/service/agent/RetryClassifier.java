package com.quantedge.backend.service.agent;

/** Decides whether a tool failure is worth retrying or should fail fast. */
public interface RetryClassifier {

    boolean isRetryable(Throwable failure);
}
