package com.bemock.prep.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a request conflicts with current state (e.g. insufficient stock, duplicate). Maps to 409. */
public class ConflictException extends ApiException {

    public ConflictException(String message) {
        super(HttpStatus.CONFLICT, message);
    }
}
