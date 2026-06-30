package com.capitec.securefile.common.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class HttpExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    ResponseEntity<ErrorResponse> handleResponseStatusException(
            ResponseStatusException ex,
            HttpServletRequest request) {
        HttpStatus status = HttpStatus.valueOf(ex.getStatusCode().value());
        return buildResponse(status, ex.getReason(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .sorted(Comparator.comparing(FieldError::getField))
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining("; "));
        if (message.isBlank()) {
            message = ex.getBindingResult().getGlobalErrors().stream()
                    .sorted(Comparator.comparing(ObjectError::getObjectName).thenComparing(ObjectError::getDefaultMessage))
                    .map(ObjectError::getDefaultMessage)
                    .collect(Collectors.joining("; "));
        }
        List<ErrorResponse.ValidationError> validationErrors = ex.getBindingResult().getAllErrors().stream()
                .map(this::formatValidationError)
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, validationErrors);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    ResponseEntity<ErrorResponse> handleConstraintViolationException(
            ConstraintViolationException ex,
            HttpServletRequest request) {
        String message = ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(this::formatConstraintViolation)
                .collect(Collectors.joining("; "));
        List<ErrorResponse.ValidationError> validationErrors = ex.getConstraintViolations().stream()
                .sorted(Comparator.comparing(violation -> violation.getPropertyPath().toString()))
                .map(violation -> ErrorResponse.ValidationError.builder()
                        .field(violation.getPropertyPath().toString())
                        .message(violation.getMessage())
                        .build())
                .toList();
        return buildResponse(HttpStatus.BAD_REQUEST, message, request, validationErrors);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatchException(
            MethodArgumentTypeMismatchException ex,
            HttpServletRequest request) {
        String message = "Invalid value for %s".formatted(ex.getName());
        return buildResponse(HttpStatus.BAD_REQUEST, message, request);
    }

    @ExceptionHandler(AuthenticationException.class)
    ResponseEntity<ErrorResponse> handleAuthenticationException(
            AuthenticationException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "Authentication failed", request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    ResponseEntity<ErrorResponse> handleAccessDeniedException(
            AccessDeniedException ex,
            HttpServletRequest request) {
        return buildResponse(HttpStatus.FORBIDDEN, "Access denied", request);
    }

    @ExceptionHandler(Exception.class)
    ResponseEntity<ErrorResponse> handleException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception for {} {}", request.getMethod(), request.getRequestURI(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected server error", request);
    }

    private String formatConstraintViolation(ConstraintViolation<?> violation) {
        return "%s %s".formatted(violation.getPropertyPath(), violation.getMessage());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message == null || message.isBlank() ? status.getReasonPhrase() : message)
                        .path(request.getRequestURI())
                        .method(request.getMethod())
                        .build());
    }

    private ResponseEntity<ErrorResponse> buildResponse(
            HttpStatus status,
            String message,
            HttpServletRequest request,
            List<ErrorResponse.ValidationError> validationErrors) {
        return ResponseEntity.status(status)
                .body(ErrorResponse.builder()
                        .timestamp(OffsetDateTime.now(ZoneOffset.UTC))
                        .status(status.value())
                        .error(status.getReasonPhrase())
                        .message(message == null || message.isBlank() ? status.getReasonPhrase() : message)
                        .path(request.getRequestURI())
                        .method(request.getMethod())
                        .validationErrors(validationErrors)
                        .build());
    }

    private ErrorResponse.ValidationError formatValidationError(ObjectError error) {
        String field = error instanceof FieldError fieldError ? fieldError.getField() : error.getObjectName();
        return ErrorResponse.ValidationError.builder()
                .field(field)
                .message(error.getDefaultMessage())
                .build();
    }

}
