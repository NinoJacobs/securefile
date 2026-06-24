package com.capitec.securefile.controller;

import com.capitec.securefile.api.AdminStatementsApi;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.service.AdminStatementsService;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@Tag(name = "Admin")
public class AdminStatementsController implements AdminStatementsApi {
    private final AdminStatementsService adminStatementsService;

    @GetMapping("/customers")
    @Override
    public ResponseEntity<List<AdminCustomerResponse>> listCustomers() {
        return ResponseEntity.ok(adminStatementsService.listCustomers());
    }
}
