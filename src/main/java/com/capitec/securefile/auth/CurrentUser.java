package com.capitec.securefile.auth;

import com.capitec.securefile.auth.security.SecurefilePrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

public final class CurrentUser {

    private CurrentUser() {
    }

    public static String username() {
        return principal().username();
    }

    public static List<String> roles() {
        return principal().roles();
    }

    public static Long customerId() {
        return principal().customerId();
    }

    public static Long requiredCustomerId() {
        Long customerId = customerId();
        if (customerId == null) {
            throw new IllegalStateException("JWT does not contain customerId");
        }
        return customerId;
    }

    public static String customerNumber() {
        return principal().customerNumber();
    }

    public static String requiredCustomerNumber() {
        String customerNumber = customerNumber();
        if (customerNumber == null || customerNumber.isBlank()) {
            throw new IllegalStateException("JWT does not contain customerNumber");
        }
        return customerNumber;
    }

    public static boolean hasRole(String role) {
        String normalizedRole = role.startsWith("ROLE_") ? role : "ROLE_" + role;
        return roles().contains(normalizedRole);
    }

    private static SecurefilePrincipal principal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new IllegalStateException("No authenticated user present in security context");
        }
        if (authentication.getPrincipal() instanceof SecurefilePrincipal principal) {
            return principal;
        }
        throw new IllegalStateException("Authenticated principal is not a SecurefilePrincipal");
    }
}
