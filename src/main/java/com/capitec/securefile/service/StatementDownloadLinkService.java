package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.Optional;

import static org.springframework.http.HttpStatus.FORBIDDEN;

@Service
public class StatementDownloadLinkService {

    private static final int DOWNLOAD_LINK_TTL_MINUTES = 1;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Value("${securefile.download-link.secret:local-dev-download-link-secret-change-me}")
    private String downloadLinkSecret;

    public DownloadLinkResponse refreshDownloadLink(Statement statement) {
        LocalDateTime expiresAt = LocalDateTime.now(ZoneOffset.UTC).plusMinutes(DOWNLOAD_LINK_TTL_MINUTES);
        statement.setDownloadLinkExpiresAt(expiresAt);
        String token = createDownloadToken(statement.getId(), statement.getCustomer().getId(), expiresAt.toEpochSecond(ZoneOffset.UTC));
        String url = "/api/v1/customers/me/statements/%s/download?token=%s".formatted(statement.getId(), token);
        return DownloadLinkResponse.builder()
                .statementId(statement.getId().toString())
                .url(url)
                .expiresAt(expiresAt.atOffset(ZoneOffset.UTC))
                .build();
    }

    public void validateDownloadToken(String token, Statement statement, Long customerId) {
        LocalDateTime expiresAt = Optional.ofNullable(statement.getDownloadLinkExpiresAt())
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Download link has not been issued"));

        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        if (!sign(payload).equals(parts[1])) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        String[] payloadParts = payload.split(":", 3);
        if (payloadParts.length != 3) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        Long tokenStatementId = parseLong(payloadParts[0])
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Invalid download token"));
        Long tokenCustomerId = parseLong(payloadParts[1])
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Invalid download token"));
        Long tokenExpiresAt = parseLong(payloadParts[2])
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Invalid download token"));

        long currentExpiresAt = expiresAt.toEpochSecond(ZoneOffset.UTC);
        if (!statement.getId().equals(tokenStatementId) || !customerId.equals(tokenCustomerId) || currentExpiresAt != tokenExpiresAt) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }
        if (LocalDateTime.now(ZoneOffset.UTC).isAfter(expiresAt)) {
            throw new ResponseStatusException(FORBIDDEN, "Download link has expired");
        }
    }

    private String createDownloadToken(Long statementId, Long customerId, long expiresAtEpochSeconds) {
        String payload = "%d:%d:%d".formatted(statementId, customerId, expiresAtEpochSeconds);
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(downloadLinkSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign download link", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
