package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.enums.StatementStatus;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.mapper.StatementApiMapper;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
public class AdminStatementsService {

    private final CustomerRepository customerRepository;
    private final StatementRepository statementRepository;
    private final StatementApiMapper statementApiMapper;
    private final StatementDomainSupportService statementDomainSupportService;
    private final StatementDocumentService statementDocumentService;
    private final StatementObjectStorageService statementObjectStorageService;

    @Transactional(readOnly = true)
    public List<AdminCustomerResponse> listCustomers() {
        return customerRepository.findAll(Sort.by(Sort.Direction.ASC, "id")).stream()
                .map(statementApiMapper::toAdminCustomerResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<StatementSummaryResponse> listStatementsForCustomer(String customerId) {
        Customer customer = statementDomainSupportService.findCustomer(customerId);
        return statementRepository.findByCustomerIdOrderByPeriodEndDesc(customer.getId()).stream()
                .map(statementApiMapper::toStatementSummaryResponse)
                .toList();
    }

    @Transactional
    public StatementDetailResponse generateStatement(String customerId) {
        Customer customer = statementDomainSupportService.findCustomer(customerId);

        LocalDateTime generatedAt = randomGeneratedAtWithinLast30Days();
        LocalDate periodEnd = generatedAt.toLocalDate();
        LocalDate periodStart = periodEnd.minusDays(29);
        String fileSuffix = generatedAt.toLocalDate().toString();
        String customerNumber = customer.getCustomerNumber();
        String fileKey = "statements/%s/%s-%s.pdf".formatted(customerNumber, customerNumber, UUID.randomUUID());
        String fileName = "%s-%s.pdf".formatted(customerNumber, fileSuffix);

        Statement statement = Statement.builder()
                .customer(customer)
                .statementName("Generated Statement %s".formatted(fileSuffix))
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .fileKey(fileKey)
                .fileName(fileName)
                .contentType("application/pdf")
                .status(StatementStatus.AVAILABLE)
                .generatedAt(generatedAt)
                .build();

        byte[] content = statementDocumentService.createStatementDocument(statement);
        StatementObjectStorageService.StoredStatementObject storedObject =
                statementObjectStorageService.storeStatement(fileKey, statement.getContentType(), content);

        statement.setFileSizeBytes(storedObject.fileSizeBytes());
        statement.setChecksum(storedObject.checksum());
        Statement savedStatement = statementRepository.save(statement);
        return statementApiMapper.toStatementDetailResponse(savedStatement);
    }

    private LocalDateTime randomGeneratedAtWithinLast30Days() {
        LocalDateTime now = LocalDateTime.now();
        long minutesBack = ThreadLocalRandom.current().nextLong(0, 30L * 24L * 60L + 1L);
        return now.minusMinutes(minutesBack);
    }
}
