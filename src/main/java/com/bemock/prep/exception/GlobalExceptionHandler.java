package com.bemock.prep.exception;

import com.bemock.prep.dto.ApiErrorResponse;
import com.bemock.prep.dto.ApiErrorResponse.FieldErrorResponse;
import com.fasterxml.jackson.databind.exc.InvalidFormatException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.TypeMismatchException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.lang.Nullable;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.web.ErrorResponse;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;
import java.util.List;

/**
 * Translates every exception into {@link ApiErrorResponse}.
 * <p>
 * Extends {@link ResponseEntityExceptionHandler} so all standard Spring MVC errors
 * (404 unknown route, 405, 415, malformed JSON, ...) keep their correct status, and
 * funnels them through {@link #handleExceptionInternal} to produce the same body shape.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(ApiException.class)
    public ResponseEntity<ApiErrorResponse> handleApiException(ApiException ex, WebRequest request) {
        return build(ex.getStatus(), ex.getMessage(), request, List.of());
    }

    /** Violations raised by {@code @Validated} beans outside Spring MVC's built-in method validation. */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex,
                                                                      WebRequest request) {
        List<FieldErrorResponse> fieldErrors = ex.getConstraintViolations().stream()
                .map(v -> new FieldErrorResponse(lastPathNode(v.getPropertyPath().toString()), v.getMessage()))
                .toList();
        return build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleUnexpected(Exception ex, WebRequest request) {
        log.error("Unhandled exception on {}", path(request), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred", request, List.of());
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        List<FieldErrorResponse> fieldErrors = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new FieldErrorResponse(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return toObject(build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors));
    }

    /** Constraint violations on {@code @RequestParam} / {@code @PathVariable} / {@code @RequestHeader} arguments. */
    @Override
    protected ResponseEntity<Object> handleHandlerMethodValidationException(HandlerMethodValidationException ex,
                                                                            HttpHeaders headers,
                                                                            HttpStatusCode status,
                                                                            WebRequest request) {
        List<FieldErrorResponse> fieldErrors = ex.getAllValidationResults().stream()
                .flatMap(result -> result instanceof ParameterErrors errors
                        ? errors.getFieldErrors().stream()
                                .map(fe -> new FieldErrorResponse(fe.getField(), fe.getDefaultMessage()))
                        : result.getResolvableErrors().stream()
                                .map(error -> new FieldErrorResponse(
                                        result.getMethodParameter().getParameterName(), error.getDefaultMessage())))
                .toList();
        return toObject(build(HttpStatus.BAD_REQUEST, "Validation failed", request, fieldErrors));
    }

    @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
                                                                  HttpHeaders headers,
                                                                  HttpStatusCode status,
                                                                  WebRequest request) {
        if (ex.getCause() instanceof InvalidFormatException invalid && !invalid.getPath().isEmpty()) {
            FieldErrorResponse fieldError = new FieldErrorResponse(fieldPath(invalid), invalidValueMessage(invalid));
            return toObject(build(HttpStatus.BAD_REQUEST, "Validation failed", request, List.of(fieldError)));
        }
        return toObject(build(HttpStatus.BAD_REQUEST, "Malformed request body", request, List.of()));
    }

    @Override
    protected ResponseEntity<Object> handleTypeMismatch(TypeMismatchException ex,
                                                        HttpHeaders headers,
                                                        HttpStatusCode status,
                                                        WebRequest request) {
        String message = "Parameter '%s' has invalid value '%s'".formatted(ex.getPropertyName(), ex.getValue());
        return toObject(build(HttpStatus.BAD_REQUEST, message, request, List.of()));
    }

    /** Catch-all for every other Spring MVC exception: keep Spring's status, use our body. */
    @Override
    protected ResponseEntity<Object> handleExceptionInternal(Exception ex,
                                                             @Nullable Object body,
                                                             HttpHeaders headers,
                                                             HttpStatusCode statusCode,
                                                             WebRequest request) {
        String message = ex instanceof ErrorResponse errorResponse && errorResponse.getBody().getDetail() != null
                ? errorResponse.getBody().getDetail()
                : ex.getMessage();
        ApiErrorResponse response = body(statusCode, message, request, List.of());
        return ResponseEntity.status(statusCode).headers(headers).body(response);
    }

    private ResponseEntity<ApiErrorResponse> build(HttpStatusCode status, String message, WebRequest request,
                                                   List<FieldErrorResponse> fieldErrors) {
        return ResponseEntity.status(status).body(body(status, message, request, fieldErrors));
    }

    private ApiErrorResponse body(HttpStatusCode status, String message, WebRequest request,
                                  List<FieldErrorResponse> fieldErrors) {
        return ApiErrorResponse.of(status, message, path(request), fieldErrors);
    }

    private static ResponseEntity<Object> toObject(ResponseEntity<ApiErrorResponse> response) {
        return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
    }

    private static String path(WebRequest request) {
        return request instanceof ServletWebRequest servletRequest
                ? servletRequest.getRequest().getRequestURI()
                : request.getDescription(false);
    }

    private static String fieldPath(InvalidFormatException ex) {
        return ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .reduce((a, b) -> b.startsWith("[") ? a + b : a + "." + b)
                .orElse("body");
    }

    private static String invalidValueMessage(InvalidFormatException ex) {
        Class<?> type = ex.getTargetType();
        return type != null && type.isEnum()
                ? "must be one of " + Arrays.toString(type.getEnumConstants())
                : "has invalid value '%s'".formatted(ex.getValue());
    }

    private static String lastPathNode(String propertyPath) {
        int dot = propertyPath.lastIndexOf('.');
        return dot >= 0 ? propertyPath.substring(dot + 1) : propertyPath;
    }
}
