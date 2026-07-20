package com.capitec.securefile.common.util;

import com.capitec.securefile.auth.security.SecurefilePrincipal;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserTest {

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void exposesValuesFromSecurefilePrincipal() {
        setAuthenticatedPrincipal(new SecurefilePrincipal("customer.one", List.of("ROLE_CUSTOMER"), 1L, "CUST-0001"));

        assertThat(CurrentUser.username()).isEqualTo("customer.one");
        assertThat(CurrentUser.roles()).containsExactly("ROLE_CUSTOMER");
        assertThat(CurrentUser.customerId()).isEqualTo(1L);
        assertThat(CurrentUser.requiredCustomerId()).isEqualTo(1L);
        assertThat(CurrentUser.customerNumber()).isEqualTo("CUST-0001");
        assertThat(CurrentUser.requiredCustomerNumber()).isEqualTo("CUST-0001");
    }

    @Test
    void hasRoleAcceptsPrefixedAndBareRoleNames() {
        setAuthenticatedPrincipal(new SecurefilePrincipal("admin.user", List.of("ROLE_ADMIN"), null, null));

        assertThat(CurrentUser.hasRole("ADMIN")).isTrue();
        assertThat(CurrentUser.hasRole("ROLE_ADMIN")).isTrue();
        assertThat(CurrentUser.hasRole("CUSTOMER")).isFalse();
    }

    @Test
    void requiredCustomerIdThrowsWhenClaimIsMissing() {
        setAuthenticatedPrincipal(new SecurefilePrincipal("admin.user", List.of("ROLE_ADMIN"), null, "CUST-0001"));

        assertThatThrownBy(CurrentUser::requiredCustomerId)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT does not contain customerId");
    }

    @Test
    void requiredCustomerNumberThrowsWhenClaimIsBlank() {
        setAuthenticatedPrincipal(new SecurefilePrincipal("customer.one", List.of("ROLE_CUSTOMER"), 1L, " "));

        assertThatThrownBy(CurrentUser::requiredCustomerNumber)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("JWT does not contain customerNumber");
    }

    @Test
    void throwsWhenNoAuthenticatedUserExists() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(CurrentUser::username)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("No authenticated user present in security context");
    }

    @Test
    void throwsWhenPrincipalIsWrongType() {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken("plain-user", "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);

        assertThatThrownBy(CurrentUser::username)
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("Authenticated principal is not a SecurefilePrincipal");
    }

    private void setAuthenticatedPrincipal(SecurefilePrincipal principal) {
        TestingAuthenticationToken authentication = new TestingAuthenticationToken(principal, "credentials");
        authentication.setAuthenticated(true);
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }
}
