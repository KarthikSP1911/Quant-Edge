package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class EmailAlreadyInUseException extends ApiException {

    public EmailAlreadyInUseException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
