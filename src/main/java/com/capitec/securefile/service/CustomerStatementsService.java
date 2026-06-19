package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.mapper.StatementApiMapper;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
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

@Service
@RequiredArgsConstructor
public class CustomerStatementsService {

    private final StatementRepository statementRepository;
    private final StatementApiMapper statementApiMapper;
    private final StatementDomainSupportService statementDomainSupportService;
    private final StatementDownloadLinkService statementDownloadLinkService;
    private final StatementDocumentService statementDocumentService;
    private final StatementObjectStorageService statementObjectStorageService;

    @Transactional
    public List<StatementSummaryResponse> listMyStatements() {
        Customer customer = statementDomainSupportService.getCurrentCustomer();
        return statementRepository.findByCustomerIdOrderByPeriodEndDesc(customer.getId()).stream()
                .map(this::toStatementSummaryResponseWithRefreshedDownloadLink)
                .toList();
    }

    @Transactional
    public StatementDetailResponse getMyStatement(String statementId) {
        Customer customer = statementDomainSupportService.getCurrentCustomer();
        Statement statement = statementDomainSupportService.findStatementForCustomer(statementId, customer.getId());
        DownloadLinkResponse downloadLink = refreshDownloadLink(statement);
        return statementApiMapper.toStatementDetailResponse(statement, downloadLink);
    }

    @Transactional
    public ResponseEntity<Resource> downloadStatement(String statementId, String token) {
        Customer customer = statementDomainSupportService.getCurrentCustomer();
        Statement statement = statementDomainSupportService.findStatementForCustomer(statementId, customer.getId());
        statementDownloadLinkService.validateDownloadToken(token, statement, customer.getId());

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

        byte[] generatedContent = statementDocumentService.createStatementDocument(statement);
        statementObjectStorageService.storeStatement(statement.getFileKey(), statement.getContentType(), generatedContent);
        return generatedContent;
    }
}
