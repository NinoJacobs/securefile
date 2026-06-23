package com.capitec.securefile.common.exception;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class ErrorResponse {
    int status;
    String error;
    String message;
    String path;
    OffsetDateTime timestamp;
}
