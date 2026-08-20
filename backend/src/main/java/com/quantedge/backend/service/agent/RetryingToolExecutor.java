package com.quantedge.backend.service.agent;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.quantedge.backend.config.AgentGuardrailProperties;
import org.springframework.stereotype.Component;

/**
 * Wraps a single tool invocation with the agent's timeout and retry guardrails, so every
 * read-only tool the research agent can call fails predictably (a bounded wait, a bounded number
 * of attempts) instead of hanging the loop or bubbling an unhandled exception into the LLM
 * conversation.
 */
@Component
public class RetryingToolExecutor {

    private final AgentGuardrailProperties guardrails;

    public RetryingToolExecutor(AgentGuardrailProperties guardrails) {
        this.guardrails = guardrails;
    }

    /**
     * Runs {@code action} with a per-attempt timeout, retrying transient failures up to the
     * configured max. Returns the action's result, or throws the last failure once retries are
     * exhausted - callers are expected to catch this and turn it into a tool-result string, the
     * same way the rest of the codebase's {@code @Tool} methods report failures to the model.
     */
    public <T> T execute(Callable<T> action) throws Exception {
        int attempts = 0;
        Exception lastFailure = null;

        while (attempts <= guardrails.getToolMaxRetries()) {
            attempts++;
            try {
                return runWithTimeout(action);
            } catch (Exception e) {
                lastFailure = e;
            }
        }

        throw lastFailure;
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
