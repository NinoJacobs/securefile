package com.capitec.securefile.common.exception;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;
import java.util.List;

@Value
@Builder
public class ErrorResponse {
    OffsetDateTime timestamp;
    int status;
    String error;
    String message;
    String path;
    String method;
    List<ValidationError> validationErrors;

    @Value
    @Builder
    public static class ValidationError {
        String field;
        String message;
    }
}
