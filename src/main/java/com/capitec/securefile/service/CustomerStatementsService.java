package com.capitec.securefile.service;

import com.capitec.securefile.common.util.CurrentUser;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.common.mapper.StatementApiMapper;
import com.capitec.securefile.model.request.StatementPeriod;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import com.capitec.securefile.storage.service.StatementObjectStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class CustomerStatementsService {

    private final StatementRepository statementRepository;
    private final StatementApiMapper statementApiMapper;
    private final StatementDomainSupportService statementDomainSupportService;
    private final StatementDownloadLinkService statementDownloadLinkService;
    private final StatementGenerationService statementGenerationService;
    private final StatementObjectStorageService statementObjectStorageService;

    @Transactional
    public List<StatementSummaryResponse> listMyStatements() {
        Long customerId = CurrentUser.requiredCustomerId();
        return statementRepository.findByCustomerIdOrderByPeriodEndDesc(customerId).stream()
                .map(this::toStatementSummaryResponseWithRefreshedDownloadLink)
                .toList();
    }

    @Transactional
    public StatementDetailResponse requestMyStatement(StatementPeriod period, LocalDate startDate, LocalDate endDate) {
        Long customerId = CurrentUser.requiredCustomerId();
        Statement statement = statementGenerationService.generateStatement(customerId, period, startDate, endDate);
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
        Long customerId = CurrentUser.requiredCustomerId();
        Statement statement = statementDomainSupportService.findStatementForCustomer(statementId, customerId);
        statementDownloadLinkService.validateDownloadToken(token, statement, customerId);

        byte[] content = loadOrCreateStatementObject(statement);
        Resource resource = new ByteArrayResource(content);
        MediaType mediaType = MediaType.parseMediaType(statement.getContentType());
        return ResponseEntity.ok()
                .contentType(mediaType)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                        .filename(statement.getFileName())
                        .build()
                        .toString())
                .body(resource);
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
