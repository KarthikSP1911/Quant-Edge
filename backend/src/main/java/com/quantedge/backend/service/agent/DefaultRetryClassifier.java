package com.quantedge.backend.service.agent;

import java.io.IOException;

import com.quantedge.backend.exception.CompanyNotFoundException;
import com.quantedge.backend.exception.ExternalApiException;
import com.quantedge.backend.exception.RateLimitExceededException;
import com.quantedge.backend.service.agent.RetryingToolExecutor.ToolTimeoutException;
import org.springframework.stereotype.Component;

/**
 * Retryable: timeouts, rate limits, and upstream provider errors ({@link ExternalApiException}
 * wraps Finnhub/Alpha Vantage 5xx/network failures as a 502) - these are transient by nature and a
 * second attempt has a real chance of succeeding. Terminal: anything else, notably
 * {@link CompanyNotFoundException} and validation-style failures, where retrying the exact same
 * call can only waste the tool's retry budget on a failure that will happen every time.
 */
@Component
public class DefaultRetryClassifier implements RetryClassifier {

    @Override
    public boolean isRetryable(Throwable failure) {
        if (failure instanceof CompanyNotFoundException) {
            return false;
        }
        return failure instanceof ToolTimeoutException
                || failure instanceof RateLimitExceededException
                || failure instanceof ExternalApiException
                || failure instanceof IOException;
    }
}
