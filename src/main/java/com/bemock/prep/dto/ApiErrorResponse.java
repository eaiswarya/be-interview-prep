package com.bemock.prep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Instant;
import java.util.List;

/**
 * The single error body returned by every endpoint, whatever went wrong.
 * {@code fieldErrors} is only present for validation failures.
 */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiErrorResponse(
        Instant timestamp,
        int status,
        String error,
        String message,
        String path,
        List<FieldErrorResponse> fieldErrors) {

    public record FieldErrorResponse(String field, String message) {
    }
}
