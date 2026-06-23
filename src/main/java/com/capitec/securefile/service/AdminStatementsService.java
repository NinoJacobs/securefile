package com.capitec.securefile.service;

import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.common.mapper.StatementApiMapper;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminStatementsService {

    private final CustomerRepository customerRepository;
    private final StatementApiMapper statementApiMapper;

    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> listCustomers() {
        return customerRepository.findAllByOrderByIdAsc().stream()
                .map(statementApiMapper::toAdminCustomerResponse)
                .toList();
    }


}
