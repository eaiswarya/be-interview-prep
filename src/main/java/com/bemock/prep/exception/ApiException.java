package com.bemock.prep.exception;

import org.springframework.http.HttpStatus;

/**
 * Base class for business exceptions thrown from the service layer.
 * Each subclass carries the HTTP status it maps to, so the handler needs no per-type branching.
 */
public abstract class ApiException extends RuntimeException {

    private final HttpStatus status;

    protected ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
