package com.quantedge.backend.service.agent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.quantedge.backend.config.AgentGuardrailProperties;
import org.springframework.stereotype.Component;

/**
 * Wraps a single tool invocation with the agent's timeout and retry guardrails, so every
 * read-only tool the research agent can call fails predictably (a bounded wait, a bounded number
 * of attempts) instead of hanging the loop or bubbling an unhandled exception into the LLM
 * conversation.
 *
 * <p>Only failures {@link RetryClassifier} calls retryable get a second attempt, with exponential
 * backoff between attempts; everything else fails immediately. Tool methods return a bare value
 * (not a richer result type), so the number of attempts a call actually used is published via
 * {@link #lastAttemptCount()} on this same (virtual) thread right before {@link #execute} returns
 * - callers that want to record a "succeeded after retry" outcome should read it immediately after
 * calling {@link #execute}, before doing anything else on the thread.
 */
@Component
public class RetryingToolExecutor {

    private static final ThreadLocal<Integer> LAST_ATTEMPT_COUNT = new ThreadLocal<>();

    private final AgentGuardrailProperties guardrails;
    private final RetryClassifier retryClassifier;

    public RetryingToolExecutor(AgentGuardrailProperties guardrails, RetryClassifier retryClassifier) {
        this.guardrails = guardrails;
        this.retryClassifier = retryClassifier;
    }

    /** The attempt count used by the most recent {@link #execute} call on this thread, or {@code null}. */
    public static Integer lastAttemptCount() {
        return LAST_ATTEMPT_COUNT.get();
    }

    /**
     * Runs {@code action} with a per-attempt timeout, retrying retryable failures (with backoff) up
     * to the configured max. Returns the action's result, or throws the last failure once retries
     * are exhausted or a terminal failure is hit - callers are expected to catch this and turn it
     * into a tool-result string, the same way the rest of the codebase's {@code @Tool} methods
     * report failures to the model.
     */
    public <T> T execute(Callable<T> action) throws Exception {
        int attempts = 0;
        Exception lastFailure = null;

        while (attempts <= guardrails.getToolMaxRetries()) {
            attempts++;
            try {
                T result = runWithTimeout(action);
                LAST_ATTEMPT_COUNT.set(attempts);
                return result;
            } catch (Exception e) {
                lastFailure = e;
                if (!retryClassifier.isRetryable(e)) {
                    break;
                }
                if (attempts <= guardrails.getToolMaxRetries()) {
                    sleepBackoff(attempts);
                }
            }
        }

        LAST_ATTEMPT_COUNT.set(attempts);
        throw lastFailure;
    }

    private void sleepBackoff(int attempt) {
        long base = guardrails.getToolRetryBackoffMillis();
        long delay = base * (1L << (attempt - 1)) + ThreadLocalRandom.current().nextLong(base);
        try {
            Thread.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private <T> T runWithTimeout(Callable<T> action) throws Exception {
        CompletableFuture<T> future =
                CompletableFuture.supplyAsync(() -> call(action), Executors.newVirtualThreadPerTaskExecutor());
        try {
            return future.get(guardrails.getToolTimeoutSeconds(), TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            future.cancel(true);
            throw new ToolTimeoutException("Tool call timed out after " + guardrails.getToolTimeoutSeconds() + "s");
        } catch (ExecutionException e) {
            Throwable cause = e.getCause();
            if (cause instanceof WrappedCheckedException wrapped) {
                throw wrapped.original;
            }
            if (cause instanceof Exception causeException) {
                throw causeException;
            }
            throw e;
        }
    }

    private <T> T call(Callable<T> action) {
        try {
            return action.call();
        } catch (Exception e) {
            throw new WrappedCheckedException(e);
        }
    }

    private static class WrappedCheckedException extends RuntimeException {
        private final Exception original;

        WrappedCheckedException(Exception original) {
            super(original);
            this.original = original;
        }
    }

    public static class ToolTimeoutException extends RuntimeException {
        public ToolTimeoutException(String message) {
            super(message);
        }
    }
}
