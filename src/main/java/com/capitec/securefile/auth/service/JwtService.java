package com.capitec.securefile.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.capitec.securefile.database.entity.Customer;
import org.springframework.beans.factory.annotation.Value;
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
import java.util.HashMap;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };

    private final ObjectMapper objectMapper;
    private final String jwtSecret;
    private final long ttlMinutes;

    public JwtService(
            ObjectMapper objectMapper,
            @Value("${securefile.jwt.secret:local-dev-jwt-secret-change-me}") String jwtSecret,
            @Value("${securefile.jwt.ttl-minutes:60}") long ttlMinutes) {
        this.objectMapper = objectMapper;
        this.jwtSecret = jwtSecret;
        this.ttlMinutes = ttlMinutes;
    }

    public IssuedToken issueToken(UserDetails userDetails, Customer customer) {
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(ttlMinutes);
        List<String> roles = userDetails.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .toList();

        Map<String, Object> claims = new HashMap<>();
        claims.put("sub", userDetails.getUsername());
        claims.put("roles", roles);
        claims.put("exp", expiresAt.toEpochSecond());
        if (customer != null) {
            claims.put("customerId", customer.getId());
            claims.put("customerNumber", customer.getCustomerNumber());
        }

        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encodeJson(claims);
        String unsignedToken = header + "." + payload;
        return new IssuedToken(unsignedToken + "." + sign(unsignedToken), expiresAt);
    }

    public ValidatedToken validateToken(String token) {
        String[] parts = token.split("\\.", 3);
        if (parts.length != 3) {
            throw unauthorized("Invalid JWT token");
        }

        String unsignedToken = parts[0] + "." + parts[1];
        if (!sign(unsignedToken).equals(parts[2])) {
            throw unauthorized("Invalid JWT token");
        }

        Map<String, Object> payload = decodeJson(parts[1]);
        String username = stringClaim(payload, "sub");
        long expiresAt = longClaim(payload, "exp");
        if (Instant.now().getEpochSecond() > expiresAt) {
            throw unauthorized("JWT token has expired");
        }

        Object rolesClaim = payload.get("roles");
        if (!(rolesClaim instanceof List<?> values)) {
            throw unauthorized("Invalid JWT token");
        }
        List<String> roles = values.stream()
                .filter(String.class::isInstance)
                .map(String.class::cast)
                .toList();
        Long customerId = optionalLongClaim(payload, "customerId");
        String customerNumber = optionalStringClaim(payload, "customerNumber");
        return new ValidatedToken(username, roles, customerId, customerNumber);
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

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
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

    public record ValidatedToken(String username, List<String> roles, Long customerId, String customerNumber) {
    }
}
