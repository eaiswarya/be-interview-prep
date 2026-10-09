package com.bemock.prep.exception;

import org.springframework.http.HttpStatus;

/** Thrown for business-rule violations that bean validation cannot express. Maps to 400. */
public class BadRequestException extends ApiException {

    public BadRequestException(String message) {
        super(HttpStatus.BAD_REQUEST, message);
    }
}
