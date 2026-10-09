package com.bemock.prep.exception;

import org.springframework.http.HttpStatus;

/** Thrown when credentials are wrong. Maps to 401. */
public class UnauthorizedException extends ApiException {

    public UnauthorizedException(String message) {
        super(HttpStatus.UNAUTHORIZED, message);
    }
}
