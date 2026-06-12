package com.capitec.securefile.controller;

import com.capitec.securefile.api.StatementGenerationRequestsApi;
import com.capitec.securefile.model.request.CreateGenerationRequestRequest;
import com.capitec.securefile.model.response.GenerationRequestResponse;
import com.capitec.securefile.service.StatementApiService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/statements/generation-requests")
@RequiredArgsConstructor
public class StatementGenerationRequestController implements StatementGenerationRequestsApi {

    private final StatementApiService statementApiService;

    @PostMapping
    @Override
    public ResponseEntity<GenerationRequestResponse> createGenerationRequest(
            @Valid @RequestBody CreateGenerationRequestRequest request) {
        return ResponseEntity.status(201).body(statementApiService.createGenerationRequest(request));
    }

    @GetMapping("/{requestId}")
    @Override
    public ResponseEntity<GenerationRequestResponse> getGenerationRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(statementApiService.getGenerationRequest(requestId));
    }

    @PostMapping("/{requestId}/retry")
    @Override
    public ResponseEntity<GenerationRequestResponse> retryGenerationRequest(@PathVariable String requestId) {
        return ResponseEntity.ok(statementApiService.retryGenerationRequest(requestId));
    }
}
