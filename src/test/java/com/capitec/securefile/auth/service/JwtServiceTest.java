package com.capitec.securefile.auth.service;

import com.capitec.securefile.auth.config.JwtProperties;
import com.capitec.securefile.database.entity.Customer;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            new ObjectMapper(),
            new JwtProperties(
                    "access-secret-for-tests-1234567890",
                    5,
                    "refresh-secret-for-tests-1234567890",
                    1440
            )
    );

    @Test
    void shouldIssueAndValidateAccessToken() {
        User userDetails = new User(
                "customer.one",
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );
        Customer customer = Customer.builder().id(1L).customerNumber("CUST-0001").build();

        JwtService.IssuedToken accessToken = jwtService.issueAccessToken(userDetails, customer);

        JwtService.ValidatedAccessToken validatedToken = jwtService.validateAccessToken(accessToken.token());

        assertThat(validatedToken.username()).isEqualTo("customer.one");
        assertThat(validatedToken.roles()).containsExactly("ROLE_CUSTOMER");
        assertThat(validatedToken.customerId()).isEqualTo(1L);
        assertThat(validatedToken.customerNumber()).isEqualTo("CUST-0001");
    }

    @Test
    void shouldIssueAndValidateRefreshToken() {
        User userDetails = new User(
                "customer.one",
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        JwtService.IssuedToken refreshToken = jwtService.issueRefreshToken(userDetails);

        JwtService.ValidatedRefreshToken validatedToken = jwtService.validateRefreshToken(refreshToken.token());

        assertThat(validatedToken.username()).isEqualTo("customer.one");
    }

    @Test
    void shouldRejectRefreshTokenWhenUsedAsAccessToken() {
        User userDetails = new User(
                "customer.one",
                "encoded-password",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER"))
        );

        JwtService.IssuedToken refreshToken = jwtService.issueRefreshToken(userDetails);

        assertThatThrownBy(() -> jwtService.validateAccessToken(refreshToken.token()))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("401 UNAUTHORIZED");
    }
}
