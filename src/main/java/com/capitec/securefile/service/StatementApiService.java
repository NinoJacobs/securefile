package com.capitec.securefile.service;

import com.capitec.securefile.database.entity.Customer;
import com.capitec.securefile.database.entity.Statement;
import com.capitec.securefile.database.entity.StatementGenerationRequest;
import com.capitec.securefile.database.enums.GenerationRequestStatus;
import com.capitec.securefile.database.enums.StatementStatus;
import com.capitec.securefile.database.repository.CustomerRepository;
import com.capitec.securefile.database.repository.StatementGenerationRequestRepository;
import com.capitec.securefile.database.repository.StatementRepository;
import com.capitec.securefile.model.request.CreateGenerationRequestRequest;
import com.capitec.securefile.model.request.GenerateStatementRequest;
import com.capitec.securefile.model.response.DownloadLinkResponse;
import com.capitec.securefile.model.response.GenerationRequestResponse;
import com.capitec.securefile.model.response.StatementDetailResponse;
import com.capitec.securefile.model.response.StatementSummaryResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Slf4j
@Service
@RequiredArgsConstructor
public class StatementApiService {

    private static final int DOWNLOAD_LINK_TTL_MINUTES = 15;
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final CustomerRepository customerRepository;
    private final StatementRepository statementRepository;
    private final StatementGenerationRequestRepository statementGenerationRequestRepository;

    @Value("${securefile.storage.statement-directory:storage/statements}")
    private Path statementStorageDirectory;

    @Value("${securefile.download-link.secret:local-dev-download-link-secret-change-me}")
    private String downloadLinkSecret;

    @Transactional(readOnly = true)
    public List<StatementSummaryResponse> listMyStatements() {
        Customer customer = getCurrentCustomer();
        return statementRepository.findByCustomerIdOrderByPeriodEndDesc(customer.getId()).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public StatementDetailResponse getMyStatement(String statementId) {
        Customer customer = getCurrentCustomer();
        Statement statement = findStatementForCustomer(statementId, customer.getId());
        DownloadLinkResponse downloadLink = createDownloadLink(statement);
        return toDetailResponse(statement, downloadLink);
    }

    @Transactional(readOnly = true)
    public DownloadLinkResponse createDownloadLink(String statementId) {
        Customer customer = getCurrentCustomer();
        Statement statement = findStatementForCustomer(statementId, customer.getId());
        return createDownloadLink(statement);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<Resource> downloadStatement(String statementId, String token) {
        Customer customer = getCurrentCustomer();
        Statement statement = findStatementForCustomer(statementId, customer.getId());
        validateDownloadToken(token, statement.getId(), customer.getId());

        Path filePath = statementStorageDirectory.resolve(statement.getFileKey()).normalize();
        if (!filePath.startsWith(statementStorageDirectory.normalize())) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid statement file path");
        }
        if (!Files.exists(filePath) || !Files.isRegularFile(filePath)) {
            throw new ResponseStatusException(NOT_FOUND, "Statement file not found");
        }

        try {
            Resource resource = new UrlResource(filePath.toUri());
            MediaType mediaType = MediaType.parseMediaType(statement.getContentType());
            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.attachment()
                            .filename(statement.getFileName())
                            .build()
                            .toString())
                    .body(resource);
        } catch (MalformedURLException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to read statement file", ex);
        }
    }

    @Transactional(readOnly = true)
    public List<StatementSummaryResponse> listStatementsForCustomer(String customerId) {
        Customer customer = findCustomer(customerId);
        return statementRepository.findByCustomerIdOrderByPeriodEndDesc(customer.getId()).stream()
                .map(this::toSummaryResponse)
                .toList();
    }

    @Transactional
    public StatementDetailResponse uploadStatement(
            String customerId,
            MultipartFile file,
            String statementName,
            String periodStart,
            String periodEnd) {
        Customer customer = findCustomer(customerId);
        validatePdf(file);

        String originalFilename = Optional.ofNullable(file.getOriginalFilename())
                .map(StringUtils::cleanPath)
                .filter(name -> !name.isBlank())
                .orElse("statement.pdf");
        String storedFilename = UUID.randomUUID() + "-" + originalFilename;
        String fileKey = Path.of(customer.getCustomerNumber(), storedFilename).toString();
        Path destination = statementStorageDirectory.resolve(fileKey).normalize();

        if (!destination.startsWith(statementStorageDirectory.normalize())) {
            throw new ResponseStatusException(BAD_REQUEST, "Invalid file name");
        }

        try {
            Files.createDirectories(destination.getParent());
            file.transferTo(destination);
        } catch (IOException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to store statement file", ex);
        }

        Statement statement = new Statement();
        statement.setCustomer(customer);
        statement.setStatementName(statementName);
        statement.setPeriodStart(parseDate(periodStart, "periodStart"));
        statement.setPeriodEnd(parseDate(periodEnd, "periodEnd"));
        statement.setFileKey(fileKey);
        statement.setFileName(originalFilename);
        statement.setFileSizeBytes(file.getSize());
        statement.setContentType(MediaType.APPLICATION_PDF_VALUE);
        statement.setStatus(StatementStatus.AVAILABLE);
        statement.setGeneratedAt(LocalDateTime.now());

        Statement savedStatement = statementRepository.save(statement);
        return toDetailResponse(savedStatement, null);
    }

    @Transactional
    public GenerationRequestResponse generateStatement(String customerId, GenerateStatementRequest request) {
        Customer customer = findCustomer(customerId);

        StatementGenerationRequest generationRequest = new StatementGenerationRequest();
        generationRequest.setCustomer(customer);
        generationRequest.setStatus(GenerationRequestStatus.PENDING);
        generationRequest.setPeriodStart(request.periodStart());
        generationRequest.setPeriodEnd(request.periodEnd());

        StatementGenerationRequest savedRequest = statementGenerationRequestRepository.save(generationRequest);
        return toGenerationRequestResponse(savedRequest, request.statementType());
    }

    public GenerationRequestResponse createGenerationRequest(CreateGenerationRequestRequest request) {
        GenerateStatementRequest generateRequest = new GenerateStatementRequest(
                request.periodStart(),
                request.periodEnd(),
                request.statementType());
        return generateStatement(request.customerId(), generateRequest);
    }

    @Transactional(readOnly = true)
    public GenerationRequestResponse getGenerationRequest(String requestId) {
        Long id = parseLong(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement generation request not found"));
        StatementGenerationRequest request = statementGenerationRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement generation request not found"));
        return toGenerationRequestResponse(request, "ACCOUNT_STATEMENT");
    }

    public GenerationRequestResponse retryGenerationRequest(String requestId) {
        Long id = parseLong(requestId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement generation request not found"));
        StatementGenerationRequest request = statementGenerationRequestRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement generation request not found"));
        request.setStatus(GenerationRequestStatus.PENDING);
        request.setErrorMessage(null);
        return toGenerationRequestResponse(statementGenerationRequestRepository.save(request), "ACCOUNT_STATEMENT");
    }

    private Customer getCurrentCustomer() {
        // Temporary until authentication exists. Keeps /customers/me usable with seeded data.
        return customerRepository.findFirstByOrderByIdAsc()
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "No customer found"));
    }

    private Customer findCustomer(String customerId) {
        return parseLong(customerId)
                .flatMap(customerRepository::findById)
                .or(() -> customerRepository.findByCustomerNumber(customerId))
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Customer not found"));
    }

    private Statement findStatementForCustomer(String statementId, Long customerId) {
        Long id = parseLong(statementId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement not found"));

        return statementRepository.findByIdAndCustomerId(id, customerId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Statement not found"));
    }

    private DownloadLinkResponse createDownloadLink(Statement statement) {
        OffsetDateTime expiresAt = OffsetDateTime.now(ZoneOffset.UTC).plusMinutes(DOWNLOAD_LINK_TTL_MINUTES);
        String token = createDownloadToken(statement.getId(), statement.getCustomer().getId(), expiresAt.toEpochSecond());
        String url = "/api/v1/customers/me/statements/%s/download?token=%s".formatted(statement.getId(), token);
        return new DownloadLinkResponse(statement.getId().toString(), url, expiresAt);
    }

    private String createDownloadToken(Long statementId, Long customerId, long expiresAtEpochSeconds) {
        String payload = "%d:%d:%d".formatted(statementId, customerId, expiresAtEpochSeconds);
        return base64Url(payload.getBytes(StandardCharsets.UTF_8)) + "." + sign(payload);
    }

    private void validateDownloadToken(String token, Long statementId, Long customerId) {
        String[] parts = token.split("\\.", 2);
        if (parts.length != 2) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        String payload;
        try {
            payload = new String(Base64.getUrlDecoder().decode(parts[0]), StandardCharsets.UTF_8);
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        if (!sign(payload).equals(parts[1])) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        String[] payloadParts = payload.split(":", 3);
        if (payloadParts.length != 3) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }

        Long tokenStatementId = parseLong(payloadParts[0])
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Invalid download token"));
        Long tokenCustomerId = parseLong(payloadParts[1])
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Invalid download token"));
        Long expiresAtEpochSeconds = parseLong(payloadParts[2])
                .orElseThrow(() -> new ResponseStatusException(FORBIDDEN, "Invalid download token"));

        if (!statementId.equals(tokenStatementId) || !customerId.equals(tokenCustomerId)) {
            throw new ResponseStatusException(FORBIDDEN, "Invalid download token");
        }
        if (Instant.now().getEpochSecond() > expiresAtEpochSeconds) {
            throw new ResponseStatusException(FORBIDDEN, "Download token has expired");
        }
    }

    private String sign(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(downloadLinkSecret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return base64Url(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to sign download link", ex);
        }
    }

    private String base64Url(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private void validatePdf(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(BAD_REQUEST, "Statement PDF is required");
        }

        String contentType = file.getContentType();
        String filename = file.getOriginalFilename();
        boolean hasPdfContentType = MediaType.APPLICATION_PDF_VALUE.equalsIgnoreCase(contentType);
        boolean hasPdfExtension = filename != null && filename.toLowerCase().endsWith(".pdf");
        if (!hasPdfContentType && !hasPdfExtension) {
            throw new ResponseStatusException(BAD_REQUEST, "Only PDF statements are supported");
        }
    }

    private LocalDate parseDate(String value, String fieldName) {
        try {
            return LocalDate.parse(value);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(BAD_REQUEST, fieldName + " must be an ISO date, for example 2026-01-31");
        }
    }

    private StatementSummaryResponse toSummaryResponse(Statement statement) {
        return new StatementSummaryResponse(
                statement.getId().toString(),
                statement.getCustomer().getId().toString(),
                maskCustomerNumber(statement.getCustomer().getCustomerNumber()),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                toOffsetDateTime(statement.getGeneratedAt()),
                statement.getStatus());
    }

    private StatementDetailResponse toDetailResponse(Statement statement, DownloadLinkResponse downloadLink) {
        return new StatementDetailResponse(
                statement.getId().toString(),
                statement.getCustomer().getId().toString(),
                maskCustomerNumber(statement.getCustomer().getCustomerNumber()),
                statement.getPeriodStart(),
                statement.getPeriodEnd(),
                toOffsetDateTime(statement.getGeneratedAt()),
                statement.getStatus(),
                statement.getFileName(),
                statement.getFileSizeBytes() == null ? 0 : statement.getFileSizeBytes(),
                statement.getContentType(),
                downloadLink == null ? null : downloadLink.url(),
                downloadLink == null ? null : downloadLink.expiresAt());
    }

    private GenerationRequestResponse toGenerationRequestResponse(
            StatementGenerationRequest request,
            String statementType) {
        return new GenerationRequestResponse(
                request.getId().toString(),
                request.getStatement() == null ? null : request.getStatement().getId().toString(),
                request.getCustomer().getId().toString(),
                statementType,
                request.getPeriodStart(),
                request.getPeriodEnd(),
                request.getStatus(),
                toOffsetDateTime(request.getRequestedAt()),
                toOffsetDateTime(request.getUpdatedAt()));
    }

    private OffsetDateTime toOffsetDateTime(LocalDateTime value) {
        return value == null ? null : value.atOffset(ZoneOffset.UTC);
    }

    private String maskCustomerNumber(String customerNumber) {
        if (customerNumber == null || customerNumber.length() <= 4) {
            return "****";
        }
        return "****" + customerNumber.substring(customerNumber.length() - 4);
    }

    private Optional<Long> parseLong(String value) {
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }
}
