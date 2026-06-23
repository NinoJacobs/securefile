package com.capitec.securefile.storage.config;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "securefile.s3")
public record SecurefileS3Properties(
        @NotBlank String endpoint,
        @NotBlank String region,
        @NotBlank String bucket,
        @NotBlank String accessKey,
        @NotBlank String secretKey,
        boolean pathStyleAccessEnabled
) {
}
