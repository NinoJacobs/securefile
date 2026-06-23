package com.capitec.securefile.auth.security;

import java.util.List;

public record SecurefilePrincipal(
        String username,
        List<String> roles,
        Long customerId,
        String customerNumber) {
}
