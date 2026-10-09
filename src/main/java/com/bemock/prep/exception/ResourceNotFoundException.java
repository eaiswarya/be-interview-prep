package com.bemock.prep.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a requested entity does not exist. Maps to 404. */
public class ResourceNotFoundException extends ApiException {

    public ResourceNotFoundException(String resource, Object id) {
        super(HttpStatus.NOT_FOUND, "%s with id %s not found".formatted(resource, id));
    }

    public ResourceNotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
