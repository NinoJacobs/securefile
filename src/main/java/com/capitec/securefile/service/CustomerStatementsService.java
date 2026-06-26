package com.capitec.securefile.service;

import com.capitec.securefile.common.mapper.StatementApiMapper;
import com.capitec.securefile.common.util.CurrentUser;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.model.request.StatementPeriod;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.storage.service.StatementObjectStorageService;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CustomerStatementsService {

    private static final String CUSTOM_STATEMENT_NAME = "Custom Statement";

    private final StatementRepository statementRepository;
    private final StatementApiMapper statementApiMapper;
    private final StatementDomainSupportService statementDomainSupportService;
    private final StatementDownloadLinkService statementDownloadLinkService;
    private final StatementGenerationService statementGenerationService;
    private final StatementObjectStorageService statementObjectStorageService;
    private final MeterRegistry meterRegistry;

    @PostConstruct
    void registerMetrics() {
        meterRegistry.counter("securefile_statement_generation");
        meterRegistry.counter("securefile_statement_download");
        meterRegistry.counter("securefile_statement_download_failures");
    }

    @Transactional
    public List<StatementSummaryResponse> listMyStatements() {
        Long customerId = CurrentUser.requiredCustomerId();
        return statementRepository.findByCustomerIdOrderByPeriodEndDesc(customerId).stream()
                .filter(statement -> !CUSTOM_STATEMENT_NAME.equals(statement.getStatementName()))
                .map(this::toStatementSummaryResponseWithRefreshedDownloadLink)
                .toList();
    }

    @Transactional
    public StatementDetailResponse requestMyStatement(StatementPeriod period, LocalDate startDate, LocalDate endDate) {
        Long customerId = CurrentUser.requiredCustomerId();
        Statement statement = statementGenerationService.generateStatement(customerId, period, startDate, endDate);
        meterRegistry.counter("securefile_statement_generation").increment();
        DownloadLinkResponse downloadLink = refreshDownloadLink(statement);
        return statementApiMapper.toStatementDetailResponse(statement, downloadLink);
    }

    @Transactional
    public StatementDetailResponse getMyStatement(String statementId) {
        Long customerId = CurrentUser.requiredCustomerId();
        Statement statement = statementDomainSupportService.findStatementForCustomer(statementId, customerId);
        DownloadLinkResponse downloadLink = refreshDownloadLink(statement);
        return statementApiMapper.toStatementDetailResponse(statement, downloadLink);
    }

    @Transactional
    public ResponseEntity<Resource> downloadStatement(String statementId, String token) {
        try {
            Long customerId = CurrentUser.requiredCustomerId();
            Statement statement = statementDomainSupportService.findStatementForCustomer(statementId, customerId);
            statementDownloadLinkService.validateDownloadToken(token, statement, customerId);

            byte[] content = loadOrCreateStatementObject(statement);
            Resource resource = new ByteArrayResource(content);
            MediaType mediaType = MediaType.parseMediaType(statement.getContentType());
            meterRegistry.counter("securefile_statement_download").increment();
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .contentLength(content.length)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(statement.getFileName())
                            .build()
                            .toString())
                    .body(resource);
        } catch (RuntimeException ex) {
            meterRegistry.counter("securefile_statement_download_failures").increment();
            throw ex;
        }
    }

    private StatementSummaryResponse toStatementSummaryResponseWithRefreshedDownloadLink(Statement statement) {
        DownloadLinkResponse downloadLink = refreshDownloadLink(statement);
        return statementApiMapper.toStatementSummaryResponse(statement, downloadLink);
    }

    private DownloadLinkResponse refreshDownloadLink(Statement statement) {
        DownloadLinkResponse downloadLink = statementDownloadLinkService.refreshDownloadLink(statement);
        statementRepository.save(statement);
        return downloadLink;
    }

    private byte[] loadOrCreateStatementObject(Statement statement) {
        if (statementObjectStorageService.statementExists(statement.getFileKey())) {
            return statementObjectStorageService.loadStatement(statement.getFileKey());
        }

        byte[] generatedContent = statementGenerationService.createStatementDocument(statement);
        StatementObjectStorageService.StoredStatementObject storedObject =
                statementObjectStorageService.storeStatement(statement.getFileKey(), statement.getContentType(), generatedContent);
        statement.setFileSizeBytes(storedObject.fileSizeBytes());
        statement.setChecksum(storedObject.checksum());
        statementRepository.save(statement);
        return generatedContent;
    }
}
