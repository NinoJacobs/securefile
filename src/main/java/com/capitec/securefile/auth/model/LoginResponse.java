package com.capitec.securefile.auth.model;

import lombok.Builder;
import lombok.Value;

import java.time.OffsetDateTime;

@Value
@Builder
public class LoginResponse {
    String tokenType;
    String accessToken;
    OffsetDateTime expiresAt;
}
