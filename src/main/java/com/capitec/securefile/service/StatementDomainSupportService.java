package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class StatementDomainSupportService {

    private final StatementRepository statementRepository;

    @Transactional(readOnly = true)
    public Statement findStatementForCustomer(Long statementId, Long customerId) {
        return statementRepository.findByIdAndCustomerId(statementId, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement not found"));
    }
}
