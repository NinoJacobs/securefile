package com.capitec.securefile.controller;

import com.capitec.securefile.api.CustomerStatementsApi;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.service.StatementApiService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/customers/me/statements")
@RequiredArgsConstructor
public class CustomerStatementsController implements CustomerStatementsApi {

    private final StatementApiService statementApiService;

    @GetMapping
    @Override
    public ResponseEntity<List<StatementSummaryResponse>> listStatements() {
        return ResponseEntity.ok(statementApiService.listMyStatements());
    }

    @GetMapping("/{statementId}")
    @Override
    public ResponseEntity<StatementDetailResponse> getStatement(@PathVariable String statementId) {
        return ResponseEntity.ok(statementApiService.getMyStatement(statementId));
    }

    @PostMapping("/{statementId}/download-link")
    @Override
    public ResponseEntity<DownloadLinkResponse> createDownloadLink(@PathVariable String statementId) {
        return ResponseEntity.status(201).body(statementApiService.createDownloadLink(statementId));
    }

    @GetMapping("/{statementId}/download")
    @Override
    public ResponseEntity<Resource> downloadStatement(
            @PathVariable String statementId,
            @RequestParam String token) {
        return statementApiService.downloadStatement(statementId, token);
    }
}
