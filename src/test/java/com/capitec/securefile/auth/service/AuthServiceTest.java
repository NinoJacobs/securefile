package com.capitec.securefile.auth.service;

import com.capitec.securefile.auth.model.LoginRequest;
import com.capitec.securefile.auth.model.LoginResponse;
import com.capitec.securefile.auth.model.RefreshTokenRequest;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Role;
import com.capitec.securefile.database.entity.User;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.UserRepository;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private SecurefileUserDetailsService userDetailsService;
    @Mock
    private UserRepository userRepository;
    @Mock
    private CustomerRepository customerRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private JwtService jwtService;

    @Test
    void loginShouldReturnAccessAndRefreshTokens() {
        AuthService authService = new AuthService(
                userDetailsService,
                userRepository,
                customerRepository,
                passwordEncoder,
                jwtService,
                new SimpleMeterRegistry()
        );

        LoginRequest request = new LoginRequest();
        request.setUsername("customer.one");
        request.setPassword("password");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("customer.one")
                .password("encoded-password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
        User user = User.builder()
                .id(1L)
                .username("customer.one")
                .role(Role.builder().name("CUSTOMER").build())
                .status("ACTIVE")
                .build();
        Customer customer = Customer.builder().id(10L).customerNumber("CUST-0001").build();
        JwtService.TokenPair tokenPair = tokenPair("access-token", "refresh-token");

        when(userDetailsService.loadUserByUsername("customer.one")).thenReturn(userDetails);
        when(passwordEncoder.matches("password", "encoded-password")).thenReturn(true);
        when(userRepository.findByUsername("customer.one")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));
        when(jwtService.issueTokenPair(userDetails, customer)).thenReturn(tokenPair);

        LoginResponse response = authService.login(request);

        assertThat(response.getAccessToken()).isEqualTo("access-token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(jwtService).issueTokenPair(userDetails, customer);
    }

    @Test
    void refreshShouldIssueNewAccessAndRefreshTokens() {
        AuthService authService = new AuthService(
                userDetailsService,
                userRepository,
                customerRepository,
                passwordEncoder,
                jwtService,
                new SimpleMeterRegistry()
        );

        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("existing-refresh-token");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername("customer.one")
                .password("encoded-password")
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")))
                .build();
        User user = User.builder()
                .id(1L)
                .username("customer.one")
                .role(Role.builder().name("CUSTOMER").build())
                .status("ACTIVE")
                .build();
        Customer customer = Customer.builder().id(10L).customerNumber("CUST-0001").build();
        JwtService.TokenPair tokenPair = tokenPair("new-access-token", "new-refresh-token");

        when(jwtService.validateRefreshToken("existing-refresh-token"))
                .thenReturn(new JwtService.ValidatedRefreshToken("customer.one"));
        when(userDetailsService.loadUserByUsername("customer.one")).thenReturn(userDetails);
        when(userRepository.findByUsername("customer.one")).thenReturn(Optional.of(user));
        when(customerRepository.findByUserId(1L)).thenReturn(Optional.of(customer));
        when(jwtService.issueTokenPair(userDetails, customer)).thenReturn(tokenPair);

        LoginResponse response = authService.refresh(request);

        assertThat(response.getAccessToken()).isEqualTo("new-access-token");
        assertThat(response.getRefreshToken()).isEqualTo("new-refresh-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");

        verify(jwtService).validateRefreshToken("existing-refresh-token");
        verify(jwtService).issueTokenPair(userDetails, customer);
    }

    private JwtService.TokenPair tokenPair(String accessToken, String refreshToken) {
        return new JwtService.TokenPair(
                new JwtService.IssuedToken(accessToken, OffsetDateTime.parse("2026-06-26T10:00:00Z")),
                new JwtService.IssuedToken(refreshToken, OffsetDateTime.parse("2026-07-03T10:00:00Z"))
        );
    }
}
