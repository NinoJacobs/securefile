package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.mapper.StatementApiMapper;
import com.capitec.securefile.model.response.AdminCustomerResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

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
        return customerRepository.findAllByOrderByIdAsc().stream()
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

        LocalDateTime generatedAt = LocalDateTime.now();
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

    @Transactional
    public StatementDetailResponse uploadStatement(String customerId, MultipartFile file) {
        Customer customer = statementDomainSupportService.findCustomer(customerId);

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "A PDF file is required");
        }

        String originalFilename = file.getOriginalFilename();
        String contentType = file.getContentType();
        if (!"application/pdf".equalsIgnoreCase(contentType) && (originalFilename == null || !originalFilename.toLowerCase().endsWith(".pdf"))) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PDF files are supported");
        }

        byte[] content;
        try {
            content = file.getBytes();
        } catch (IOException ex) {
            throw new ResponseStatusException(BAD_REQUEST, "Unable to read uploaded file", ex);
        }

        LocalDateTime uploadedAt = LocalDateTime.now();
        LocalDate periodEnd = uploadedAt.toLocalDate();
        LocalDate periodStart = periodEnd.minusDays(29);
        String customerNumber = customer.getCustomerNumber();
        String baseName = normalizePdfBaseName(originalFilename);
        String fileKey = "statements/%s/%s-%s.pdf".formatted(customerNumber, customerNumber, UUID.randomUUID());
        String fileName = baseName + ".pdf";

        Statement statement = Statement.builder()
                .customer(customer)
                .statementName(baseName)
                .periodStart(periodStart)
                .periodEnd(periodEnd)
                .fileKey(fileKey)
                .fileName(fileName)
                .contentType("application/pdf")
                .generatedAt(uploadedAt)
                .build();

        StatementObjectStorageService.StoredStatementObject storedObject =
                statementObjectStorageService.storeStatement(fileKey, statement.getContentType(), content);

        statement.setFileSizeBytes(storedObject.fileSizeBytes());
        statement.setChecksum(storedObject.checksum());
        Statement savedStatement = statementRepository.save(statement);
        return statementApiMapper.toStatementDetailResponse(savedStatement);
    }

    private String normalizePdfBaseName(String originalFilename) {
        if (originalFilename == null || originalFilename.isBlank()) {
            return "uploaded-statement";
        }

        String trimmed = originalFilename.trim();
        int extensionIndex = trimmed.toLowerCase().lastIndexOf(".pdf");
        String baseName = extensionIndex > 0 ? trimmed.substring(0, extensionIndex) : trimmed;
        return baseName.isBlank() ? "uploaded-statement" : baseName;
    }
}
