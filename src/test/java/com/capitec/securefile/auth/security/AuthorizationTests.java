package com.capitec.securefile.auth.security;

import com.capitec.securefile.auth.service.JwtService;
import com.capitec.securefile.auth.service.AuthService;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.AdminStatementsService;
import com.capitec.securefile.service.CustomerStatementsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class AuthorizationTests {

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @MockitoBean
    private JwtService jwtService;

    @MockitoBean
    private AuthService authService;

    @MockitoBean
    private CustomerStatementsService customerStatementsService;

    @MockitoBean
    private AdminStatementsService adminStatementsService;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
    }

    @Test
    void shouldRejectCustomerEndpointWithoutToken() throws Exception {
        mockMvc.perform(get("/api/v1/customers/me/statements"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Authentication required"))
                .andExpect(jsonPath("$.path").value("/api/v1/customers/me/statements"))
                .andExpect(jsonPath("$.method").value("GET"));
    }

    @Test
    void shouldRejectCustomerEndpointWithAdminToken() throws Exception {
        when(jwtService.validateAccessToken("admin-token"))
                .thenReturn(new JwtService.ValidatedAccessToken("admin.user", List.of("ROLE_ADMIN"), null, null));

        mockMvc.perform(get("/api/v1/customers/me/statements")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isForbidden())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldAllowCustomerEndpointWithCustomerToken() throws Exception {
        when(jwtService.validateAccessToken("customer-token"))
                .thenReturn(new JwtService.ValidatedAccessToken("customer.one", List.of("ROLE_CUSTOMER"), 1L, "CUST-0001"));
        when(customerStatementsService.listMyStatements()).thenReturn(List.of(StatementSummaryResponse.builder()
                .statementId("1")
                .statementName("Statement")
                .customerId("1")
                .build()));

        mockMvc.perform(get("/api/v1/customers/me/statements")
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].statementId").value("1"));
    }

    @Test
    void shouldRejectAdminEndpointWithCustomerToken() throws Exception {
        when(jwtService.validateAccessToken("customer-token"))
                .thenReturn(new JwtService.ValidatedAccessToken("customer.one", List.of("ROLE_CUSTOMER"), 1L, "CUST-0001"));

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer customer-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void shouldAllowAdminEndpointWithAdminToken() throws Exception {
        when(jwtService.validateAccessToken("admin-token"))
                .thenReturn(new JwtService.ValidatedAccessToken("admin.user", List.of("ROLE_ADMIN"), null, null));
        when(adminStatementsService.listCustomers()).thenReturn(List.of(AdminCustomerResponse.builder()
                .customerId("1")
                .customerNumber("CUST-0001")
                .build()));

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", "Bearer admin-token"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].customerId").value("1"));
    }

    @Test
    void shouldReturnJsonErrorForInvalidToken() throws Exception {
        when(jwtService.validateAccessToken(anyString()))
                .thenThrow(new ResponseStatusException(UNAUTHORIZED, "Invalid JWT token", new UsernameNotFoundException("missing")));

        mockMvc.perform(get("/api/v1/customers/me/statements")
                        .header("Authorization", "Bearer bad-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value("Invalid or expired JWT token"));
    }

    @Test
    void shouldValidateCustomStatementDateRange() throws Exception {
        when(jwtService.validateAccessToken("customer-token"))
                .thenReturn(new JwtService.ValidatedAccessToken("customer.one", List.of("ROLE_CUSTOMER"), 1L, "CUST-0001"));

        mockMvc.perform(post("/api/v1/customers/me/statements/generate")
                        .header("Authorization", "Bearer customer-token")
                        .param("period", "CUSTOM"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("startDate and endDate are required for custom statements"))
                .andExpect(jsonPath("$.validationErrors[0].message").value("startDate and endDate are required for custom statements"));
    }

}
