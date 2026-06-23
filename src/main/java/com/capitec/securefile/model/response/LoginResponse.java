package com.capitec.securefile.model.response;

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
