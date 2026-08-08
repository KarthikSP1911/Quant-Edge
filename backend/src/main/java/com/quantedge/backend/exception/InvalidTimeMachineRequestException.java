package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidTimeMachineRequestException extends ApiException {

    public InvalidTimeMachineRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
