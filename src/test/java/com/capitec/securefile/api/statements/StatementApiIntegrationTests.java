package com.capitec.securefile.api.statements;

import com.capitec.securefile.database.entity.Account;
import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Role;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.entity.User;
import com.capitec.securefile.database.repository.AccountRepository;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.RoleRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.database.repository.UserRepository;
import com.capitec.securefile.storage.service.StatementObjectStorageService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.web.context.WebApplicationContext;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Base64;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
class StatementApiIntegrationTests {

    private static final String PASSWORD = "Password123!";
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    @Autowired
    private WebApplicationContext webApplicationContext;

    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private StatementRepository statementRepository;

    @MockitoBean
    private StatementObjectStorageService statementObjectStorageService;

    @Value("${securefile.download-link.secret}")
    private String downloadLinkSecret;

    private SeedData seedData;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();
        clearData();
        seedData = seedData();
    }

    @Test
    void customerCanLogIn() throws Exception {
        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "customer.one",
                                  "password": "Password123!"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.expiresAt").isNotEmpty())
                .andExpect(jsonPath("$.refreshTokenExpiresAt").isNotEmpty());
    }

    @Test
    void customerCanListOwnStatements() throws Exception {
        String accessToken = login("customer.one", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/me/statements")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].statementId").value(seedData.customerOneStatement.getId().toString()))
                .andExpect(jsonPath("$[0].customerId").value(seedData.customerOne.getId().toString()))
                .andExpect(jsonPath("$[0].statementName").value("1 Month Statement"))
                .andExpect(jsonPath("$[0].accountNumberMasked").value("****0001"))
                .andExpect(jsonPath("$[0].downloadUrl").isNotEmpty());
    }

    @Test
    void customerCanGetOwnStatementDetail() throws Exception {
        String accessToken = login("customer.one", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/me/statements/{statementId}", seedData.customerOneStatement.getId())
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.statementId").value(seedData.customerOneStatement.getId().toString()))
                .andExpect(jsonPath("$.customerId").value(seedData.customerOne.getId().toString()))
                .andExpect(jsonPath("$.fileName").value("CUST-0001-2026-05.pdf"))
                .andExpect(jsonPath("$.contentType").value(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(jsonPath("$.downloadUrl").isNotEmpty());
    }

    @Test
    void customerCannotGetAnotherCustomersStatement() throws Exception {
        String accessToken = login("customer.one", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/me/statements/{statementId}", seedData.customerTwoStatement.getId())
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Statement not found"));
    }

    @Test
    void customerGetsBadRequestForMalformedStatementId() throws Exception {
        String accessToken = login("customer.one", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/me/statements/{statementId}", "abc")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Invalid value for statementId"));
    }

    @Test
    void adminCannotUseCustomerEndpoints() throws Exception {
        String accessToken = login("admin.user", PASSWORD);

        mockMvc.perform(get("/api/v1/customers/me/statements")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void customerCannotUseAdminEndpoints() throws Exception {
        String accessToken = login("customer.one", PASSWORD);

        mockMvc.perform(get("/api/v1/admin/customers")
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Access denied"));
    }

    @Test
    void customerCanDownloadStatementWithValidToken() throws Exception {
        String accessToken = login("customer.one", PASSWORD);
        byte[] statementBytes = "statement-bytes".getBytes(StandardCharsets.UTF_8);

        when(statementObjectStorageService.statementExists(seedData.customerOneStatement.getFileKey())).thenReturn(true);
        when(statementObjectStorageService.loadStatement(seedData.customerOneStatement.getFileKey())).thenReturn(statementBytes);

        String downloadUrl = downloadUrlFor(seedData.customerOneStatement.getId(), accessToken);

        mockMvc.perform(get(downloadUrl)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_PDF_VALUE))
                .andExpect(header().string("Content-Disposition", "attachment; filename=\"CUST-0001-2026-05.pdf\""))
                .andExpect(content().bytes(statementBytes));

        verify(statementObjectStorageService).statementExists(seedData.customerOneStatement.getFileKey());
        verify(statementObjectStorageService).loadStatement(seedData.customerOneStatement.getFileKey());
    }

    @Test
    void customerCannotDownloadStatementWithInvalidToken() throws Exception {
        String accessToken = login("customer.one", PASSWORD);
        downloadUrlFor(seedData.customerOneStatement.getId(), accessToken);

        mockMvc.perform(get("/api/v1/customers/me/statements/{statementId}/download", seedData.customerOneStatement.getId())
                        .header("Authorization", bearer(accessToken))
                        .param("token", "bad-token"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Invalid download token"));

        verifyNoInteractions(statementObjectStorageService);
    }

    @Test
    void customerCannotDownloadStatementWithExpiredToken() throws Exception {
        String accessToken = login("customer.one", PASSWORD);
        LocalDateTime expiredAt = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(1);

        seedData.customerOneStatement.setDownloadLinkExpiresAt(expiredAt);
        statementRepository.save(seedData.customerOneStatement);

        String expiredToken = createDownloadToken(
                seedData.customerOneStatement.getId(),
                seedData.customerOne.getId(),
                expiredAt);

        mockMvc.perform(get("/api/v1/customers/me/statements/{statementId}/download", seedData.customerOneStatement.getId())
                        .header("Authorization", bearer(accessToken))
                        .param("token", expiredToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value("Download link has expired"));

        verifyNoInteractions(statementObjectStorageService);
    }

    private void clearData() {
        statementRepository.deleteAllInBatch();
        accountRepository.deleteAllInBatch();
        customerRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
    }

    private SeedData seedData() {
        Role adminRole = roleRepository.save(Role.builder().name("ADMIN").build());
        Role customerRole = roleRepository.save(Role.builder().name("CUSTOMER").build());

        User adminUser = userRepository.save(user("admin.user", "admin@securefile.test", adminRole));
        User customerOneUser = userRepository.save(user("customer.one", "customer.one@securefile.test", customerRole));
        User customerTwoUser = userRepository.save(user("customer.two", "customer.two@securefile.test", customerRole));

        Customer customerOne = customerRepository.save(customer(customerOneUser, "CUST-0001"));
        Customer customerTwo = customerRepository.save(customer(customerTwoUser, "CUST-0002"));

        Account customerOneAccount = accountRepository.save(account(customerOne, "100000000001"));
        Account customerTwoAccount = accountRepository.save(account(customerTwo, "100000000002"));

        Statement customerOneStatement = statementRepository.save(statement(
                customerOne,
                customerOneAccount,
                "1 Month Statement",
                "statements/CUST-0001/2026-05.pdf",
                "CUST-0001-2026-05.pdf",
                LocalDateTime.of(2026, 6, 1, 8, 0)));

        Statement customerTwoStatement = statementRepository.save(statement(
                customerTwo,
                customerTwoAccount,
                "1 Month Statement",
                "statements/CUST-0002/2026-05.pdf",
                "CUST-0002-2026-05.pdf",
                LocalDateTime.of(2026, 6, 1, 8, 5)));

        return new SeedData(adminUser, customerOne, customerTwo, customerOneStatement, customerTwoStatement);
    }

    private User user(String username, String email, Role role) {
        return User.builder()
                .username(username)
                .email(email)
                .passwordHash(passwordEncoder.encode(PASSWORD))
                .firstName("Test")
                .lastName("User")
                .phone("+27110000000")
                .role(role)
                .status("ACTIVE")
                .build();
    }

    private Customer customer(User user, String customerNumber) {
        return Customer.builder()
                .user(user)
                .customerNumber(customerNumber)
                .status("ACTIVE")
                .build();
    }

    private Account account(Customer customer, String accountNumber) {
        return Account.builder()
                .customer(customer)
                .accountNumber(accountNumber)
                .accountType("CHEQUE")
                .currentBalance(BigDecimal.valueOf(1000))
                .status("ACTIVE")
                .build();
    }

    private Statement statement(
            Customer customer,
            Account account,
            String statementName,
            String fileKey,
            String fileName,
            LocalDateTime generatedAt) {
        return Statement.builder()
                .customer(customer)
                .account(account)
                .statementName(statementName)
                .periodStart(LocalDate.of(2026, 5, 1))
                .periodEnd(LocalDate.of(2026, 5, 31))
                .fileKey(fileKey)
                .fileName(fileName)
                .fileSizeBytes(128L)
                .contentType(MediaType.APPLICATION_PDF_VALUE)
                .checksum("checksum")
                .generatedAt(generatedAt)
                .build();
    }

    private String login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "%s",
                                  "password": "%s"
                                }
                                """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("accessToken").asText();
    }

    private String downloadUrlFor(Long statementId, String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(get("/api/v1/customers/me/statements/{statementId}", statementId)
                        .header("Authorization", bearer(accessToken)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode response = objectMapper.readTree(result.getResponse().getContentAsString());
        return response.get("downloadUrl").asText();
    }

    private String bearer(String accessToken) {
        return "Bearer " + accessToken;
    }

    private String createDownloadToken(Long statementId, Long customerId, LocalDateTime expiresAt) {
        String payload = "%d:%d:%d".formatted(statementId, customerId, expiresAt.toEpochSecond(ZoneOffset.UTC));
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(downloadLinkSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Unable to sign download token for test", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private record SeedData(
            User adminUser,
            Customer customerOne,
            Customer customerTwo,
            Statement customerOneStatement,
            Statement customerTwoStatement) {
    }
}
