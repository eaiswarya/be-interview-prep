package com.bemock.prep.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;

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

    public static ApiErrorResponse of(HttpStatusCode status, String message, String path,
                                      List<FieldErrorResponse> fieldErrors) {
        HttpStatus resolved = HttpStatus.resolve(status.value());
        String reason = resolved != null ? resolved.getReasonPhrase() : String.valueOf(status.value());
        return new ApiErrorResponse(Instant.now(), status.value(), reason, message, path, fieldErrors);
    }

    public record FieldErrorResponse(String field, String message) {
    }
}
