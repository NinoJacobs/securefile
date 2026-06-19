package com.capitec.securefile.controller;

import com.capitec.securefile.api.CustomerStatementsApi;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.CustomerStatementsService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/statements")
@RequiredArgsConstructor
public class CustomerStatementsController implements CustomerStatementsApi {

    private final CustomerStatementsService customerStatementsService;

    @GetMapping
    @Override
    public ResponseEntity<List<StatementSummaryResponse>> listStatements() {
        return ResponseEntity.ok(customerStatementsService.listMyStatements());
    }

    @GetMapping("/{statementId}")
    @Override
    public ResponseEntity<StatementDetailResponse> getStatement(@PathVariable String statementId) {
        return ResponseEntity.ok(customerStatementsService.getMyStatement(statementId));
    }

    @GetMapping("/{statementId}/download")
    @Override
    public ResponseEntity<Resource> downloadStatement(
            @PathVariable String statementId) {
        return customerStatementsService.downloadStatement(statementId);
    }
}
