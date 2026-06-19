package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
@RequiredArgsConstructor
public class StatementDomainSupportService {

    private final CustomerRepository customerRepository;
    private final StatementRepository statementRepository;

    @Transactional(readOnly = true)
    public Customer getCurrentCustomer() {
        // Temporary until authentication exists. Keeps /customers/me usable with seeded data.
        return customerRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No customer found"));
    }

    @Transactional(readOnly = true)
    public Customer findCustomer(String customerId) {
        return parseLong(customerId)
                .flatMap(customerRepository::findById)
                .or(() -> customerRepository.findByCustomerNumber(customerId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
    }

    @Transactional(readOnly = true)
    public Statement findStatementForCustomer(String statementId, Long customerId) {
        Long id = parseLong(statementId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement not found"));

        return statementRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement not found"));
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
