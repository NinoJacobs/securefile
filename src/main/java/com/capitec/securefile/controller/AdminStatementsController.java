package com.capitec.securefile.controller;

import com.capitec.securefile.api.AdminStatementsApi;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.StatementApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatementsController implements AdminStatementsApi {
    private final StatementApiService statementApiService;

    @GetMapping("/customers/{customerId}/statements")
    @Override
    public ResponseEntity<List<StatementSummaryResponse>> listCustomerStatements(@PathVariable String customerId) {
        return ResponseEntity.ok(statementApiService.listStatementsForCustomer(customerId));
    }
}
