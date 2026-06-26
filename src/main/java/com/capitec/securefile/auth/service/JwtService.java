package com.capitec.securefile.auth.service;

import com.capitec.securefile.auth.config.JwtProperties;
import com.capitec.securefile.database.entity.Customer;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final String ACCESS_TOKEN_TYPE = "ACCESS";
    private static final String REFRESH_TOKEN_TYPE = "REFRESH";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String jwtSecret;
    private final long accessTokenTtlMinutes;
    private final String refreshTokenSecret;
    private final long refreshTokenTtlMinutes;

    public JwtService(ObjectMapper objectMapper, JwtProperties properties) {
        this.objectMapper = objectMapper;
        this.jwtSecret = properties.secret();
        this.accessTokenTtlMinutes = properties.ttlMinutes();
        this.refreshTokenSecret = properties.refreshSecret();
        this.refreshTokenTtlMinutes = properties.refreshTtlMinutes();
    }

    public TokenPair issueTokenPair(UserDetails userDetails, Customer customer) {
        IssuedToken accessToken = issueAccessToken(userDetails, customer);
        IssuedToken refreshToken = issueRefreshToken(userDetails);
        return new TokenPair(accessToken, refreshToken);
    }

    public IssuedToken issueAccessToken(UserDetails userDetails, Customer customer) {
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(accessTokenTtlMinutes);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("roles", roles);
        claims.put("exp", expiresAt.toEpochSecond());
        claims.put("tokenType", ACCESS_TOKEN_TYPE);
        if (customer != null) {
            claims.put("customerId", customer.getId());
            claims.put("customerNumber", customer.getCustomerNumber());
        }

        return issueToken(claims, jwtSecret, expiresAt);
    }

    public IssuedToken issueRefreshToken(UserDetails userDetails) {
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(refreshTokenTtlMinutes);

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("exp", expiresAt.toEpochSecond());
        claims.put("tokenType", REFRESH_TOKEN_TYPE);

        return issueToken(claims, refreshTokenSecret, expiresAt);
    }

    public ValidatedAccessToken validateAccessToken(String token) {
        Map<String, Object> payload = validateAndDecodeToken(token, jwtSecret, ACCESS_TOKEN_TYPE);
        String username = stringClaim(payload, "sub");

        Object rolesClaim = payload.get("roles");
        if (!(rolesClaim instanceof List<?> values) || values.isEmpty()) {
            throw unauthorized("Invalid JWT token");
        }
        List<String> roles = values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        if (roles.isEmpty()) {
            throw unauthorized("Invalid JWT token");
        }

        Long customerId = optionalLongClaim(payload, "customerId");
        String customerNumber = optionalStringClaim(payload, "customerNumber");
        return new ValidatedAccessToken(username, roles, customerId, customerNumber);
    }

    public ValidatedRefreshToken validateRefreshToken(String token) {
        Map<String, Object> payload = validateAndDecodeToken(token, refreshTokenSecret, REFRESH_TOKEN_TYPE);
        String username = stringClaim(payload, "sub");
        return new ValidatedRefreshToken(username);
    }

    private IssuedToken issueToken(Map<String, Object> claims, String secret, OffsetDateTime expiresAt) {
        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encodeJson(claims);
        String unsignedToken = header + "." + payload;
        return new IssuedToken(unsignedToken + "." + sign(unsignedToken, secret), expiresAt);
    }

    private Map<String, Object> validateAndDecodeToken(String token, String secret, String expectedTokenType) {
        String[] parts = token.split("\\.", 3);
        if (parts.length != 3) {
            throw unauthorized("Invalid JWT token");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!sign(unsignedToken, secret).equals(parts[2])) {
            throw unauthorized("Invalid JWT token");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        long expiresAt = longClaim(payload, "exp");
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw unauthorized("JWT token has expired");
        }
        if (!expectedTokenType.equals(stringClaim(payload, "tokenType"))) {
            throw unauthorized("Invalid JWT token");
        }
        return payload;
    }

    private String encodeJson(Map<String, Object> value) {
        try {
            return base64Url(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create JWT token", ex);
        }
    }

    private Map<String, Object> decodeJson(String value) {
        try {
            return objectMapper.readValue(Base64.getUrlDecoder().decode(value), MAP_TYPE);
        } catch (Exception ex) {
            throw unauthorized("Invalid JWT token");
        }
    }

    private String stringClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw unauthorized("Invalid JWT token");
    }

    private long longClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw unauthorized("Invalid JWT token");
    }

    private Long optionalLongClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        throw unauthorized("Invalid JWT token");
    }

    private String optionalStringClaim(Map<String, Object> payload, String claimName) {
        Object value = payload.get(claimName);
        if (value == null) {
            return null;
        }
        if (value instanceof String text && !text.isBlank()) {
            return text;
        }
        throw unauthorized("Invalid JWT token");
    }

    private String sign(String value, String secret) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign JWT token", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private ResponseStatusException unauthorized(String reason) {
        return new ResponseStatusException(HttpStatus.UNAUTHORIZED, reason);
    }

    public record IssuedToken(String token, OffsetDateTime expiresAt) {
    }

    public record TokenPair(IssuedToken accessToken, IssuedToken refreshToken) {
    }

    public record ValidatedAccessToken(String username, List<String> roles, Long customerId, String customerNumber) {
    }

    public record ValidatedRefreshToken(String username) {
    }
}
