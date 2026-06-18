package com.capitec.securefile.controller;

import com.capitec.securefile.api.AdminStatementsApi;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.AdminStatementsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminStatementsController implements AdminStatementsApi {
    private final AdminStatementsService adminStatementsService;

    @GetMapping("/customers")
    @Override
    public ResponseEntity<List<AdminCustomerResponse>> listCustomers() {
        return ResponseEntity.ok(adminStatementsService.listCustomers());
    }

    @GetMapping("/customers/{customerId}/statements")
    @Override
    public ResponseEntity<List<StatementSummaryResponse>> listCustomerStatements(@PathVariable String customerId) {
        return ResponseEntity.ok(adminStatementsService.listStatementsForCustomer(customerId));
    }

    @PostMapping("/customers/{customerId}/statements/generate")
    @Override
    public ResponseEntity<StatementDetailResponse> generateCustomerStatement(
            @PathVariable String customerId) {
        return ResponseEntity.status(201).body(adminStatementsService.generateStatement(customerId));
    }
}
