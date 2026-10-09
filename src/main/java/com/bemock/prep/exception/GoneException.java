package com.bemock.prep.exception;

import org.springframework.http.HttpStatus;

/** Thrown when a resource existed but is no longer available (e.g. an expired short link). Maps to 410. */
public class GoneException extends ApiException {

    public GoneException(String message) {
        super(HttpStatus.GONE, message);
    }
}
