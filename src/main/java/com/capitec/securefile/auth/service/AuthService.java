package com.capitec.securefile.auth.service;

import com.capitec.securefile.auth.model.LoginRequest;
import com.capitec.securefile.auth.model.LoginResponse;
import com.capitec.securefile.auth.model.RefreshTokenRequest;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.User;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.UserRepository;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final String BEARER_TOKEN_TYPE = "Bearer";

    private final SecurefileUserDetailsService userDetailsService;
    private final UserRepository userRepository;
    private final CustomerRepository customerRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void registerMetrics() {
        meterRegistry.counter("securefile_auth_login_attempts");
        meterRegistry.counter("securefile_auth_login_failures");
        meterRegistry.counter("securefile_auth_rate_limit_blocks");
        meterRegistry.counter("securefile_auth_token_refresh");
    }

    public LoginResponse login(LoginRequest request) {
        meterRegistry.counter("securefile_auth_login_attempts").increment();
        try {
            if (request == null || request.getUsername() == null || request.getPassword() == null) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
            }

            UserDetails userDetails;
            try {
                userDetails = userDetailsService.loadUserByUsername(request.getUsername());
            } catch (UsernameNotFoundException ex) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password", ex);
            }

            if (!userDetails.isEnabled() || !passwordEncoder.matches(request.getPassword(), userDetails.getPassword())) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
            }

            Customer customer = customerForToken(userDetails);
            JwtService.TokenPair tokenPair = jwtService.issueTokenPair(userDetails, customer);
            return toLoginResponse(tokenPair);
        } catch (ResponseStatusException ex) {
            meterRegistry.counter("securefile_auth_login_failures").increment();
            throw ex;
        }
    }

    public LoginResponse refresh(RefreshTokenRequest request) {
        if (request == null || request.getRefreshToken() == null || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        JwtService.ValidatedRefreshToken validatedRefreshToken;
        try {
            validatedRefreshToken = jwtService.validateRefreshToken(request.getRefreshToken());
        } catch (ResponseStatusException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", ex);
        }

        UserDetails userDetails;
        try {
            userDetails = userDetailsService.loadUserByUsername(validatedRefreshToken.username());
        } catch (UsernameNotFoundException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token", ex);
        }

        if (!userDetails.isEnabled()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token");
        }

        Customer customer = customerForToken(userDetails);
        JwtService.TokenPair tokenPair = jwtService.issueTokenPair(userDetails, customer);
        meterRegistry.counter("securefile_auth_token_refresh").increment();
        return toLoginResponse(tokenPair);
    }

    private Customer customerForToken(UserDetails userDetails) {
        boolean isCustomer = userDetails.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_CUSTOMER".equals(authority.getAuthority()));
        if (!isCustomer) {
            return null;
        }

        User user = userRepository.findByUsername(userDetails.getUsername())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password"));
        return customerRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Customer profile not found"));
    }

    private LoginResponse toLoginResponse(JwtService.TokenPair tokenPair) {
        return LoginResponse.builder()
                .tokenType(BEARER_TOKEN_TYPE)
                .accessToken(tokenPair.accessToken().token())
                .expiresAt(tokenPair.accessToken().expiresAt())
                .refreshToken(tokenPair.refreshToken().token())
                .refreshTokenExpiresAt(tokenPair.refreshToken().expiresAt())
                .build();
    }
}
