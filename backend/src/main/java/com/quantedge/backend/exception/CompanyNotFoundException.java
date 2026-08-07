package com.quantedge.backend.exception;

import org.springframework.http.HttpStatus;

public class CompanyNotFoundException extends ApiException {

    public CompanyNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
