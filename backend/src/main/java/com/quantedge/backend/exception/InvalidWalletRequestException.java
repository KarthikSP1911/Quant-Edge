package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class InvalidWalletRequestException extends ApiException {

    public InvalidWalletRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
