package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidComparisonRequestException extends ApiException {

    public InvalidComparisonRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
