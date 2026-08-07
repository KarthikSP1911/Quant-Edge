package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when an upstream market-data provider (Finnhub, Twelve Data) fails or times out. */
public class ExternalApiException extends ApiException {

    public ExternalApiException(String message) {
        super(HttpStatus.BAD_GATEWAY, message);
    }

    public ExternalApiException(String message, Throwable cause) {
        super(HttpStatus.BAD_GATEWAY, message);
        initCause(cause);
    }
}
