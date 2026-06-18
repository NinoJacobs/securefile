//package com.capitec.securefile.api.statements;
//
//import org.junit.jupiter.api.Test;
//import org.junit.jupiter.api.BeforeEach;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.context.SpringBootTest;
//import org.springframework.http.MediaType;
//import org.springframework.test.annotation.DirtiesContext;
//import org.springframework.test.web.servlet.setup.MockMvcBuilders;
//import org.springframework.test.web.servlet.MockMvc;
//import org.springframework.web.context.WebApplicationContext;
//
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
//import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
//import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
//
//@SpringBootTest
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
//class StatementApiIntegrationTests {
//
//    @Autowired
//    private WebApplicationContext webApplicationContext;
//
//    private MockMvc mockMvc;
//
//    @BeforeEach
//    void setUp() {
//        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext).build();
//    }
//
//    @Test
//    void shouldListCurrentCustomerStatements() throws Exception {
//        mockMvc.perform(get("/api/v1/customers/me/statements"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$[0].statementId").value("stmt-1002"))
//                .andExpect(jsonPath("$[1].statementId").value("stmt-1001"));
//    }
//
//    @Test
//    void shouldGetStatementDetail() throws Exception {
//        mockMvc.perform(get("/api/v1/customers/me/statements/stmt-1001"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.statementId").value("stmt-1001"))
//                .andExpect(jsonPath("$.fileName").value("statement-2026-01.pdf"));
//    }
//
//    @Test
//    void shouldCreateDownloadLink() throws Exception {
//        mockMvc.perform(post("/api/v1/customers/me/statements/stmt-1001/download-link"))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.statementId").value("stmt-1001"))
//                .andExpect(jsonPath("$.url").isNotEmpty())
//                .andExpect(jsonPath("$.expiresAt").isNotEmpty());
//    }
//
//    @Test
//    void shouldCreateGenerationRequest() throws Exception {
//        mockMvc.perform(post("/api/v1/statements/generation-requests")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                                {
//                                  "customerId": "customer-001",
//                                  "periodStart": "2026-03-01",
//                                  "periodEnd": "2026-03-31",
//                                  "statementType": "ACCOUNT_STATEMENT"
//                                }
//                                """))
//                .andExpect(status().isCreated())
//                .andExpect(jsonPath("$.requestId").isNotEmpty())
//                .andExpect(jsonPath("$.statementId").isNotEmpty())
//                .andExpect(jsonPath("$.status").value("QUEUED"));
//    }
//
//    @Test
//    void shouldValidateGenerationRequestPayload() throws Exception {
//        mockMvc.perform(post("/api/v1/statements/generation-requests")
//                        .contentType(MediaType.APPLICATION_JSON)
//                        .content("""
//                                {
//                                  "customerId": "",
//                                  "statementType": ""
//                                }
//                                """))
//                .andExpect(status().isBadRequest());
//    }
//
//    @Test
//    void shouldReturnStatementAuditForAdmin() throws Exception {
//        mockMvc.perform(get("/api/v1/admin/statements/stmt-1001/audit"))
//                .andExpect(status().isOk())
//                .andExpect(jsonPath("$.statementId").value("stmt-1001"))
//                .andExpect(jsonPath("$.events[0].action").value("STATEMENT_GENERATED"));
//    }
//}
