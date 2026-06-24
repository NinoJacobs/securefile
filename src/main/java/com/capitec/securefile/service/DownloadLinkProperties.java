package com.capitec.securefile.service;

import jakarta.validation.constraints.NotBlank;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties(prefix = "securefile.download-link")
public record DownloadLinkProperties(@NotBlank String secret) {
}
