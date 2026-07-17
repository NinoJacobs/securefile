package com.capitec.securefile.controller;

import com.capitec.securefile.api.CustomerStatementsApi;
import com.capitec.securefile.model.request.StatementGenerationRequest;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.CustomerStatementsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

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
    public ResponseEntity<StatementDetailResponse> requestStatement(@Valid @RequestBody StatementGenerationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(customerStatementsService.requestMyStatement(request));
    }

    @GetMapping("/{statementId}")
    @Override
    public ResponseEntity<StatementDetailResponse> getStatement(@PathVariable Long statementId) {
        return ResponseEntity.ok(customerStatementsService.getMyStatement(statementId));
    }

    @GetMapping("/{statementId}/download")
    @Override
    public ResponseEntity<Resource> downloadStatement(
            @PathVariable Long statementId,
            @RequestParam String token) {
        return customerStatementsService.downloadStatement(statementId, token);
    }
}
