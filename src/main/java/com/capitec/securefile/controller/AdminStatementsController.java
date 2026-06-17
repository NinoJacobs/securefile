package com.capitec.securefile.controller;

import com.capitec.securefile.api.AdminStatementsApi;
import com.capitec.securefile.model.request.GenerateStatementRequest;
import com.capitec.securefile.model.response.GenerationRequestResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.StatementApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatementsController implements AdminStatementsApi {
    private final StatementApiService statementApiService;

    // get all customer Ids

    @GetMapping("/customers/{customerId}/statements")
    @Override
    public ResponseEntity<List<StatementSummaryResponse>> listCustomerStatements(@PathVariable String customerId) {
        return ResponseEntity.ok(statementApiService.listStatementsForCustomer(customerId));
    }

    @PostMapping(
            value = "/customers/{customerId}/statements/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Override
    public ResponseEntity<StatementDetailResponse> uploadCustomerStatement(
            @PathVariable String customerId,
            @RequestParam MultipartFile file,
            @RequestParam String statementName,
            @RequestParam String periodStart,
            @RequestParam String periodEnd) {
        return ResponseEntity.status(201)
                .body(statementApiService.uploadStatement(customerId, file, statementName, periodStart, periodEnd));
    }

    @PostMapping("/customers/{customerId}/statements/generate")
    @Override
    public ResponseEntity<GenerationRequestResponse> generateCustomerStatement(
            @PathVariable String customerId,
            @Valid @RequestBody GenerateStatementRequest request) {
        return ResponseEntity.status(202).body(statementApiService.generateStatement(customerId, request));
    }
}
