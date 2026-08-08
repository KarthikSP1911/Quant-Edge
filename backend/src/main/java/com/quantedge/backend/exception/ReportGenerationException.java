package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a PDF/CSV export fails to render or its file can't be written to disk. */
public class ReportGenerationException extends ApiException {

    public ReportGenerationException(String message, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
        initCause(cause);
    }
}
