package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidOrderRequestException extends ApiException {

    public InvalidOrderRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
