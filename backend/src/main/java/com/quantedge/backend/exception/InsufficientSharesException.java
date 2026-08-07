package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class InsufficientSharesException extends ApiException {

    public InsufficientSharesException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
