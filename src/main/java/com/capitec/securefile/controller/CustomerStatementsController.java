package com.capitec.securefile.controller;

import com.capitec.securefile.api.CustomerStatementsApi;
import com.capitec.securefile.model.request.StatementPeriod;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.CustomerStatementsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/statements")
@RequiredArgsConstructor
@Tag(name = "Customer")
public class CustomerStatementsController implements CustomerStatementsApi {

    private final CustomerStatementsService customerStatementsService;

    @GetMapping
    @Override
    public ResponseEntity<List<StatementSummaryResponse>> listStatements() {
        return ResponseEntity.ok(customerStatementsService.listMyStatements());
    }

    @PostMapping("/generate")
    @Override
    public ResponseEntity<StatementDetailResponse> requestStatement(
            @RequestParam(defaultValue = "ONE_MONTH") StatementPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return ResponseEntity.status(201).body(customerStatementsService.requestMyStatement(period, startDate, endDate));
    }

    @GetMapping("/{statementId}")
    @Override
    public ResponseEntity<StatementDetailResponse> getStatement(@PathVariable String statementId) {
        return ResponseEntity.ok(customerStatementsService.getMyStatement(statementId));
    }

    @GetMapping("/{statementId}/download")
    @Override
    public ResponseEntity<Resource> downloadStatement(
            @PathVariable String statementId,
            @RequestParam String token) {
        return customerStatementsService.downloadStatement(statementId, token);
    }
}
